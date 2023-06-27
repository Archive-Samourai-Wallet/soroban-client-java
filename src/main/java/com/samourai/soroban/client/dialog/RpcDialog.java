package com.samourai.soroban.client.dialog;

import com.google.common.base.Charsets;
import com.samourai.soroban.client.SorobanPayload;
import com.samourai.soroban.client.meeting.SorobanMessageWithSender;
import com.samourai.soroban.client.rpc.RpcClientEncrypted;
import com.samourai.soroban.client.rpc.RpcMode;
import com.samourai.soroban.client.rpc.RpcSession;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.util.CallbackWithArg;
import io.reactivex.Single;
import java.security.MessageDigest;
import java.util.function.Consumer;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcDialog {
  private static final Logger log = LoggerFactory.getLogger(RpcDialog.class);

  private RpcSession rpcSession;
  private Encrypter encrypter;
  private Consumer<String> onEncryptedPayload;
  private long retryDelayMs;

  private String nextDirectory;
  private boolean exit;

  public RpcDialog(String directory, RpcSession rpcSession, Encrypter encrypter) throws Exception {
    this.rpcSession = rpcSession;
    this.encrypter = encrypter;
    this.onEncryptedPayload =
        payload -> {
          try {
            setNextDirectory(payload);
          } catch (Exception e) {
            log.error("", e);
          }
        };
    this.retryDelayMs = 2000;

    setNextDirectory(directory);
    this.exit = false;
  }

  public Single<SorobanMessageWithSender> receiveWithSender(long timeoutMs) throws Exception {
    return withRpcClientEncrypted(
        rce -> rce.receiveEncryptedWithSender(nextDirectory, timeoutMs, retryDelayMs));
  }

  public Single<String> receive(final PaymentCode paymentCodePartner, long timeoutMs)
      throws Exception {
    return withRpcClientEncrypted(
        rce -> rce.receiveEncrypted(nextDirectory, timeoutMs, paymentCodePartner, retryDelayMs));
  }

  public Single<String> sendWithSender(SorobanPayload message, PaymentCode paymentCodePartner)
      throws Exception {
    checkExit(paymentCodePartner);
    return withRpcClientEncrypted(
        rce ->
            rce.sendEncryptedWithSender(
                nextDirectory, message, paymentCodePartner, RpcMode.NORMAL));
  }

  public Single<String> send(SorobanPayload message, PaymentCode paymentCodePartner)
      throws Exception {
    return send(message.toPayload(), paymentCodePartner);
  }

  private Single<String> send(String payload, PaymentCode paymentCodePartner) throws Exception {
    checkExit(paymentCodePartner);
    return withRpcClientEncrypted(
        rce -> rce.sendEncrypted(nextDirectory, payload, paymentCodePartner, RpcMode.NORMAL));
  }

  private void checkExit(PaymentCode paymentCodePartner) throws Exception {
    if (exit) {
      sendError("Canceled by user", paymentCodePartner).subscribe();
      throw new SorobanException("Canceled by user");
    }
  }

  public Single<String> sendError(String message, PaymentCode paymentCodePartner) throws Exception {
    return withRpcClientEncrypted(rce -> rce.sendError(nextDirectory, message, paymentCodePartner));
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
      log.trace("nextDirectory: " + this.nextDirectory);
    }
  }

  protected <R> R withRpcClientEncrypted(CallbackWithArg<RpcClientEncrypted, R> callable)
      throws Exception {
    return rpcSession.withRpcClientEncrypted(
        encrypter,
        rce -> {
          rce.setOnEncryptedPayload(onEncryptedPayload);
          return callable.apply(rce);
        });
  }

  public void close() {
    exit = true;
    rpcSession.close();
  }

  public void setRetryDelayMs(long retryDelayMs) {
    this.retryDelayMs = retryDelayMs;
  }
}
