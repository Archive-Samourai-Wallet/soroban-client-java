package com.samourai.soroban.client.rpc;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcClientEncrypted {
  private static final Logger log = LoggerFactory.getLogger(RpcClientEncrypted.class);
  private static final String ERROR_PREFIX = "ERROR:";

  private static final Z85 z85 = Z85.getInstance();
  private RpcClient rpcClient;
  private Encrypter encrypter;
  private Consumer<String> onEncryptedPayload;

  public RpcClientEncrypted(
      RpcClient rpcClient, Encrypter encrypter, Consumer<String> onEncryptedPayload) {
    this.rpcClient = rpcClient;
    this.encrypter = encrypter;
    this.onEncryptedPayload = onEncryptedPayload;
  }

  public Single<Collection<SorobanMessageWithSender>> listWithSender(String directory)
      throws Exception {
    return rpcClient
        .directoryValues(directory)
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
    return new SorobanMessageWithSender(sender, payload);
  }

  public Single<SorobanMessageWithSender> receiveEncryptedWithSender(
      String directory, long timeoutMs) throws Exception {
    return doReceiveEncrypted(directory, timeoutMs).map(this::decryptSorobanMessageWithSender);
  }

  public Single<String> receiveEncrypted(
      String directory, long timeoutMs, final PaymentCode paymentCodePartner) throws Exception {
    return doReceiveEncrypted(directory, timeoutMs)
        .map(
            payload -> {
              // decrypt
              String decryptedPayload = decrypt(payload, paymentCodePartner);
              if (log.isDebugEnabled()) {
                log.debug("<= received (" + RpcClient.shortDirectory(directory) + ")");
              }

              // check for error
              String error = getError(decryptedPayload);
              if (error != null) {
                throw new SorobanException("Partner failed with error: " + error);
              }
              return decryptedPayload;
            });
  }

  private Single<String> doReceiveEncrypted(String directory, long timeoutMs) throws Exception {
    return rpcClient
        .directoryValueWaitAndRemove(directory, timeoutMs)
        .map(
            payload -> {
              if (log.isDebugEnabled()) {
                log.debug("<= received (" + RpcClient.shortDirectory(directory) + ")");
              }
              if (onEncryptedPayload != null) {
                onEncryptedPayload.accept(payload);
              }
              return payload;
            });
  }

  public Completable sendEncryptedWithSender(
      String directory, SorobanPayload message, PaymentCode paymentCodePartner) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("=> sendWithSender (" + RpcClient.shortDirectory(directory) + ")");
    }

    // encrypt
    String encryptedPayload = encrypt(message.toPayload(), paymentCodePartner);

    // wrap with clear sender
    PaymentCode paymentCodeMine = encrypter.getPaymentCode();
    SorobanMessageWithSender messageWithSender =
        new SorobanMessageWithSender(paymentCodeMine.toString(), encryptedPayload);
    return doSendEncrypted(directory, messageWithSender.toPayload());
  }

  public Completable sendEncrypted(String directory, String payload, PaymentCode paymentCodePartner)
      throws Exception {
    // encrypt
    String encryptedPayload = encrypt(payload, paymentCodePartner);
    return doSendEncrypted(directory, encryptedPayload);
  }

  private Completable doSendEncrypted(String directory, final String payload) throws Exception {
    return send(directory, payload)
        .doOnComplete(
            () -> {
              if (onEncryptedPayload != null) {
                onEncryptedPayload.accept(payload);
              }
            });
  }

  public Completable send(String directory, final String payload) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("=> send (" + directory + ")");
    }
    return Completable.fromSingle(
        rpcClient.directoryAdd(directory, payload, RpcMode.normal.name()));
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

  public Completable sendError(String directory, String message, PaymentCode paymentCodePartner) {
    // send error
    try {
      return sendEncrypted(directory, ERROR_PREFIX + message, paymentCodePartner);
    } catch (Exception e) {
      // non-blocking error
      log.error("=> error", e);
      return Completable.error(e);
    }
  }

  public RpcClient getRpcClient() {
    return rpcClient;
  }

  public PaymentCode getPaymentCode() {
    return encrypter.getPaymentCode();
  }

  public void exit() {
    rpcClient.exit();
  }
}
