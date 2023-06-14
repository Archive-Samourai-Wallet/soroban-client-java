package com.samourai.soroban.client.dialog;

import com.google.common.base.Charsets;
import com.samourai.soroban.client.SorobanPayload;
import com.samourai.soroban.client.meeting.SorobanMessageWithSender;
import com.samourai.soroban.client.rpc.RpcClient;
import com.samourai.soroban.client.rpc.RpcClientEncrypted;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.security.MessageDigest;
import java.util.function.Consumer;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcDialog {
  private static final Logger log = LoggerFactory.getLogger(RpcDialog.class);

  private RpcClientEncrypted rpc;

  private String nextDirectory;
  private boolean exit;

  public RpcDialog(String directory, RpcClient rpcClient, Encrypter encrypter) throws Exception {
    Consumer<String> setNextDirectoryConsumer =
        payload -> {
          try {
            setNextDirectory(payload);
          } catch (Exception e) {
            log.error("", e);
          }
        };
    this.rpc = new RpcClientEncrypted(rpcClient, encrypter, setNextDirectoryConsumer);

    setNextDirectory(directory);
    this.exit = false;
  }

  public Single<SorobanMessageWithSender> receiveWithSender(long timeoutMs) throws Exception {
    return rpc.receiveEncryptedWithSender(nextDirectory, timeoutMs);
  }

  public Single<String> receive(final PaymentCode paymentCodePartner, long timeoutMs)
      throws Exception {
    return rpc.receiveEncrypted(nextDirectory, timeoutMs, paymentCodePartner);
  }

  public Completable sendWithSender(SorobanPayload message, PaymentCode paymentCodePartner)
      throws Exception {
    checkExit(paymentCodePartner);
    return rpc.sendEncryptedWithSender(nextDirectory, message, paymentCodePartner);
  }

  public Completable send(SorobanPayload message, PaymentCode paymentCodePartner) throws Exception {
    return send(message.toPayload(), paymentCodePartner);
  }

  private Completable send(String payload, PaymentCode paymentCodePartner) throws Exception {
    checkExit(paymentCodePartner);
    return rpc.sendEncrypted(nextDirectory, payload, paymentCodePartner);
  }

  private void checkExit(PaymentCode paymentCodePartner) throws Exception {
    if (exit) {
      sendError("Canceled by user", paymentCodePartner).subscribe();
      throw new SorobanException("Canceled by user");
    }
  }

  public Completable sendError(String message, PaymentCode paymentCodePartner) {
    return rpc.sendError(nextDirectory, message, paymentCodePartner);
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

  public void close() {
    exit = true;
    rpc.getRpcClient().exit();
  }
}
