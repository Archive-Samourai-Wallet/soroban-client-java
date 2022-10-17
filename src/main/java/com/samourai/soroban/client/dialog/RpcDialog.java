package com.samourai.soroban.client.dialog;

import com.google.common.base.Charsets;
import com.samourai.soroban.client.SorobanMessage;
import com.samourai.soroban.client.meeting.SorobanMessageWithSender;
import com.samourai.soroban.client.rpc.RpcClient;
import com.samourai.soroban.client.rpc.RpcMode;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.util.Z85;
import io.reactivex.Single;
import java.security.MessageDigest;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcDialog {
  private static final Logger log = LoggerFactory.getLogger(RpcDialog.class);
  private static final Z85 z85 = Z85.getInstance();
  private static final String ERROR_PREFIX = "ERROR:";

  private RpcClient rpc;
  private Encrypter encrypter;
  private String info;

  private String nextDirectory;
  private boolean exit;

  public RpcDialog(RpcClient rpc, Encrypter encrypter, String info, String directory)
      throws Exception {
    this.rpc = rpc;
    this.encrypter = encrypter;
    this.info = info;

    setNextDirectory(directory);
    this.exit = false;
  }

  private String shortNextDirectory() {
    return RpcClient.shortDirectory(nextDirectory);
  }

  public Single<SorobanMessageWithSender> receiveWithSender(long timeoutMs) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug(info + "watchingWithSender: " + shortNextDirectory());
    }
    return doReceive(timeoutMs)
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
                log.debug(info + "<= received (" + shortNextDirectory() + ")");
              }

              // return clear object
              return new SorobanMessageWithSender(sender, payload);
            });
  }

  public Single<String> receive(final PaymentCode paymentCodePartner, long timeoutMs)
      throws Exception {
    return doReceive(timeoutMs)
        .map(
            payload -> {
              // decrypt
              String decryptedPayload = decrypt(payload, paymentCodePartner);
              if (log.isDebugEnabled()) {
                log.debug(info + "<= received (" + shortNextDirectory() + ")");
              }

              // check for error
              String error = getError(decryptedPayload);
              if (error != null) {
                throw new SorobanException("Partner failed with error: " + error);
              }
              return decryptedPayload;
            });
  }

  private Single<String> doReceive(long timeoutMs) throws Exception {
    if (exit) {
      throw new Exception("Canceled by user");
    }
    if (log.isDebugEnabled()) {
      log.debug(info + "watching: " + shortNextDirectory());
    }
    return rpc.directoryValueWaitAndRemove(nextDirectory, timeoutMs)
        .map(
            payload -> {
              setNextDirectory(payload);
              return payload;
            });
  }

  public Single sendWithSender(
      SorobanMessage message, PaymentCode paymentCodeMine, PaymentCode paymentCodePartner)
      throws Exception {
    checkExit(paymentCodePartner);

    if (log.isDebugEnabled()) {
      log.debug(info + "=> sendWithSender (" + shortNextDirectory() + ")");
    }

    // encrypt
    String encryptedPayload = encrypt(message.toPayload(), paymentCodePartner);

    // wrap with clear sender
    SorobanMessageWithSender messageWithSender =
        new SorobanMessageWithSender(paymentCodeMine.toString(), encryptedPayload);
    return doSend(messageWithSender.toPayload());
  }

  public Single send(SorobanMessage message, PaymentCode paymentCodePartner) throws Exception {
    return send(message.toPayload(), paymentCodePartner);
  }

  private Single send(String payload, PaymentCode paymentCodePartner) throws Exception {
    checkExit(paymentCodePartner);
    if (log.isDebugEnabled()) {
      log.debug(info + "=> send (" + shortNextDirectory() + ")");
    }

    // encrypt
    String encryptedPayload = encrypt(payload, paymentCodePartner);
    return doSend(encryptedPayload);
  }

  protected Single doSend(final String payload) throws Exception {
    return rpc.directoryAdd(nextDirectory, payload, RpcMode.normal.name())
        .map(
            o -> {
              setNextDirectory(payload);
              return o;
            });
  }

  private void checkExit(PaymentCode paymentCodePartner) throws Exception {
    if (exit) {
      sendError("Canceled by user", paymentCodePartner).subscribe();
      throw new SorobanException("Canceled by user");
    }
  }

  public Single sendError(String message, PaymentCode paymentCodePartner) {
    // send error
    try {
      return send(ERROR_PREFIX + message, paymentCodePartner);
    } catch (Exception e) {
      // non-blocking error
      log.error(info + "=> error", e);
      return Single.just("error");
    }
  }

  private static String encodeDirectory(String payload) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(payload.getBytes(Charsets.UTF_8));
    String result = Hex.toHexString(hash);
    if (result.isEmpty()) {
      throw new Exception("Invalid encodeDirectory value");
    }
    return result;
  }

  private void setNextDirectory(String nextDirectory) throws Exception {
    this.nextDirectory = encodeDirectory(nextDirectory);
    if (log.isTraceEnabled()) {
      log.trace(info + "nextDirectory: " + this.nextDirectory);
    }
  }

  private String encrypt(String payload, PaymentCode paymentCodePartner) throws Exception {
    String encryptedPayload = z85.encode(encrypter.encrypt(payload, paymentCodePartner));
    return encryptedPayload;
  }

  private String decrypt(String encryptedPayload, PaymentCode paymentCodePartner) throws Exception {
    return encrypter.decrypt(z85.decode(encryptedPayload), paymentCodePartner);
  }

  private String getError(String payload) {
    if (payload != null && payload.startsWith(ERROR_PREFIX)) {
      return payload.substring(ERROR_PREFIX.length());
    }
    return null;
  }

  public void close() {
    exit = true;
    rpc.exit();
  }
}
