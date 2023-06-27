package com.samourai.soroban.client.rpc;

import com.samourai.http.client.IHttpClient;
import com.samourai.soroban.client.SorobanPayload;
import com.samourai.soroban.client.dialog.Encrypter;
import com.samourai.soroban.client.dialog.SorobanException;
import com.samourai.soroban.client.meeting.SorobanMessageWithSender;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.util.Z85;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.Collection;
import java.util.LinkedList;
import java.util.function.Consumer;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcClientEncrypted extends RpcClient {
  private static final Logger log = LoggerFactory.getLogger(RpcClientEncrypted.class);
  private static final String ERROR_PREFIX = "ERROR:";

  private static final Z85 z85 = Z85.getInstance();
  private Encrypter encrypter;
  private Consumer<String> onEncryptedPayload;

  public RpcClientEncrypted(
      IHttpClient httpClient, String url, NetworkParameters params, Encrypter encrypter) {
    super(httpClient, url, params);
    this.encrypter = encrypter;
    this.onEncryptedPayload = null;
  }

  public void setOnEncryptedPayload(Consumer<String> onEncryptedPayload) {
    this.onEncryptedPayload = onEncryptedPayload;
  }

  public Single<Collection<SorobanMessageWithSender>> listWithSender(String directory)
      throws Exception {
    return directoryValues(directory)
        .map(
            payloads -> {
              Collection<SorobanMessageWithSender> results = new LinkedList<>();
              for (String payload : payloads) {
                try {
                  results.add(decryptSorobanMessageWithSender(payload));
                } catch (Exception e) {
                  log.error("listWithSender: could not decrypt payload, skipping: " + payload, e);
                }
              }
              if (log.isTraceEnabled()) {
                log.trace(
                    "<= list "
                        + RpcClient.shortDirectory(directory)
                        + ": "
                        + results.size()
                        + " entries: "
                        + results);
              }
              return results;
            });
  }

  private SorobanMessageWithSender decryptSorobanMessageWithSender(String payloadWithSender)
      throws Exception {
    // read paymentCode from sender
    SorobanMessageWithSender messageWithSender = SorobanMessageWithSender.parse(payloadWithSender);
    String encryptedPayload = messageWithSender.getPayload();
    String sender = messageWithSender.getSender();
    PaymentCode paymentCodePartner = new PaymentCode(sender);

    // decrypt
    String payload = decrypt(encryptedPayload, paymentCodePartner);

    // return clear object
    return new SorobanMessageWithSender(sender, payload, payloadWithSender);
  }

  public Single<SorobanMessageWithSender> receiveEncryptedWithSender(
      String directory, long timeoutMs, long retryDelayMs) throws Exception {
    return doReceiveEncrypted(directory, timeoutMs, retryDelayMs)
        .map(
            encrypted -> {
              SorobanMessageWithSender messageWithSender =
                  decryptSorobanMessageWithSender(encrypted);
              if (log.isTraceEnabled()) {
                log.trace(
                    "<= receive "
                        + RpcClient.shortDirectory(directory)
                        + ": "
                        + messageWithSender.toString());
              }
              return messageWithSender;
            });
  }

  public Single<String> receiveEncrypted(
      String directory, long timeoutMs, final PaymentCode paymentCodePartner, long retryDelayMs)
      throws Exception {
    return doReceiveEncrypted(directory, timeoutMs, retryDelayMs)
        .map(
            payload -> {
              // decrypt
              String decryptedPayload = decrypt(payload, paymentCodePartner);
              if (log.isTraceEnabled()) {
                log.trace(
                    "<= receive " + RpcClient.shortDirectory(directory) + ": " + decryptedPayload);
              }

              // check for error
              String error = getError(decryptedPayload);
              if (error != null) {
                throw new SorobanException("Partner failed with error: " + error);
              }
              return decryptedPayload;
            });
  }

  private Single<String> doReceiveEncrypted(String directory, long timeoutMs, long retryDelayMs)
      throws Exception {
    return directoryValueWaitAndRemove(directory, timeoutMs, retryDelayMs)
        .map(
            payload -> {
              if (onEncryptedPayload != null) {
                onEncryptedPayload.accept(payload);
              }
              return payload;
            });
  }

  public Single<String> sendEncryptedWithSender(
      String directory, SorobanPayload message, PaymentCode paymentCodePartner, RpcMode rpcMode)
      throws Exception {

    // encrypt
    String encryptedPayload = encrypt(message.toPayload(), paymentCodePartner);

    // wrap with clear sender
    PaymentCode paymentCodeMine = encrypter.getPaymentCode();
    String payload =
        SorobanMessageWithSender.toPayload(paymentCodeMine.toString(), encryptedPayload);
    return doSendEncrypted(directory, payload, rpcMode).toSingle(() -> encryptedPayload);
  }

  public Single<String> sendEncrypted(
      String directory, String payload, PaymentCode paymentCodePartner, RpcMode rpcMode)
      throws Exception {
    // encrypt
    String encryptedPayload = encrypt(payload, paymentCodePartner);
    return doSendEncrypted(directory, encryptedPayload, rpcMode).toSingle(() -> encryptedPayload);
  }

  private Completable doSendEncrypted(String directory, final String payload, RpcMode rpcMode)
      throws Exception {
    return directoryAdd(directory, payload, rpcMode)
        .doOnComplete(
            () -> {
              if (onEncryptedPayload != null) {
                onEncryptedPayload.accept(payload);
              }
            });
  }

  protected String encrypt(String payload, PaymentCode paymentCodePartner) throws Exception {
    String encryptedPayload = z85.encode(encrypter.encrypt(payload, paymentCodePartner));
    return encryptedPayload;
  }

  protected String decrypt(String encryptedPayload, PaymentCode paymentCodePartner)
      throws Exception {
    return encrypter.decrypt(z85.decode(encryptedPayload), paymentCodePartner);
  }

  private String getError(String payload) {
    if (payload != null && payload.startsWith(ERROR_PREFIX)) {
      return payload.substring(ERROR_PREFIX.length());
    }
    return null;
  }

  public Single<String> sendError(
      String directory, String message, PaymentCode paymentCodePartner) {
    // send error
    try {
      return sendEncrypted(directory, ERROR_PREFIX + message, paymentCodePartner, RpcMode.NORMAL);
    } catch (Exception e) {
      // non-blocking error
      log.error("=> error", e);
      return Single.error(e);
    }
  }

  public PaymentCode getPaymentCode() {
    return encrypter.getPaymentCode();
  }
}
