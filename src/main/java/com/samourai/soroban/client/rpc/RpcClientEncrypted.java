package com.samourai.soroban.client.rpc;

import com.samourai.soroban.client.SorobanMessageSimple;
import com.samourai.soroban.client.dialog.Encrypter;
import com.samourai.soroban.client.dialog.SorobanException;
import com.samourai.soroban.client.meeting.SorobanMessageWithSender;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.util.Z85;
import io.reactivex.Single;
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

  public Single<SorobanMessageWithSender> receiveWithSender(String directory, long timeoutMs)
      throws Exception {
    return doReceive(directory, timeoutMs)
        .map(
            payloadWithSender -> {
              // read paymentCode from sender
              SorobanMessageWithSender messageWithSender =
                  SorobanMessageWithSender.parse(payloadWithSender);
              String encryptedPayload = messageWithSender.getPayload();
              String sender = messageWithSender.getSender();
              PaymentCode paymentCodePartner = new PaymentCode(sender);

              // decrypt
              String payload = decrypt(encryptedPayload, paymentCodePartner);
              if (log.isDebugEnabled()) {
                log.debug("<= received (" + RpcClient.shortDirectory(directory) + ")");
              }

              // return clear object
              return new SorobanMessageWithSender(sender, payload);
            });
  }

  public Single<String> receive(
      String directory, long timeoutMs, final PaymentCode paymentCodePartner) throws Exception {
    return doReceive(directory, timeoutMs)
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

  private Single<String> doReceive(String directory, long timeoutMs) throws Exception {
    return rpcClient
        .directoryValueWaitAndRemove(directory, timeoutMs)
        .map(
            payload -> {
              if (onEncryptedPayload != null) {
                onEncryptedPayload.accept(payload);
              }
              return payload;
            });
  }

  public Single sendWithSender(
      String directory, SorobanMessageSimple message, PaymentCode paymentCodePartner)
      throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("=> sendWithSender (" + RpcClient.shortDirectory(directory) + ")");
    }

    // encrypt
    String encryptedPayload = encrypt(message.toPayload(), paymentCodePartner);

    // wrap with clear sender
    PaymentCode paymentCodeMine = encrypter.getPaymentCode();
    SorobanMessageWithSender messageWithSender =
        new SorobanMessageWithSender(paymentCodeMine.toString(), encryptedPayload);
    return doSend(directory, messageWithSender.toPayload());
  }

  public Single send(String directory, String payload, PaymentCode paymentCodePartner)
      throws Exception {
    // encrypt
    String encryptedPayload = encrypt(payload, paymentCodePartner);
    return doSend(directory, encryptedPayload);
  }

  private Single doSend(String directory, final String payload) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("=> send (" + directory + ")");
    }
    return rpcClient
        .directoryAdd(directory, payload, RpcMode.normal.name())
        .map(
            o -> {
              if (onEncryptedPayload != null) {
                onEncryptedPayload.accept(payload);
              }
              return o;
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

  public Single sendError(String directory, String message, PaymentCode paymentCodePartner) {
    // send error
    try {
      return send(directory, ERROR_PREFIX + message, paymentCodePartner);
    } catch (Exception e) {
      // non-blocking error
      log.error("=> error", e);
      return Single.just("error");
    }
  }

  public RpcClient getRpcClient() {
    return rpcClient;
  }
}
