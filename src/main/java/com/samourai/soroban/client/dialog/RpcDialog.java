package com.samourai.soroban.client.dialog;

import com.google.common.base.Charsets;
import com.samourai.soroban.client.exception.SorobanException;
import com.samourai.soroban.client.meeting.SorobanMessageWithSender;
import com.samourai.soroban.client.rpc.RpcSession;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.sorobanClient.SorobanPayloadable;
import io.reactivex.Completable;
import java.security.MessageDigest;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcDialog {
  private static final Logger log = LoggerFactory.getLogger(RpcDialog.class);
  private static final String ERROR_PREFIX = "ERROR:";

  private RpcSession rpcSession;

  private String nextDirectory;
  private boolean exit;

  public RpcDialog(String directory, RpcSession rpcSession) throws Exception {
    this.rpcSession = rpcSession;

    setNextDirectory(directory);
    this.exit = false;
  }

  private RpcDialogEndpointWithSender computeEndpointWithSender(PaymentCode encryptTo) {
    RpcDialogEndpointWithSender endpoint =
        new RpcDialogEndpointWithSender(nextDirectory).setEncryptTo(encryptTo);
    return endpoint;
  }

  private RpcDialogEndpoint computeEndpoint(PaymentCode paymentCodePartner) {
    RpcDialogEndpoint endpoint =
        new RpcDialogEndpoint(nextDirectory)
            .setEncryptTo(paymentCodePartner)
            .setDecryptFrom(paymentCodePartner);
    return endpoint;
  }

  public SorobanMessageWithSender receiveWithSender(long timeoutMs) throws Exception {
    SorobanMessageWithSender messageWithSender =
        computeEndpointWithSender(null).loopWaitAny(rpcSession, timeoutMs);

    // decrypt
    setNextDirectory(messageWithSender.getRawEntry());

    // return clear object
    return messageWithSender;
  }

  public String receive(final PaymentCode paymentCodePartner, int timeoutMs) throws Exception {
    RpcDialogItem item = computeEndpoint(paymentCodePartner).loopWaitAny(rpcSession, timeoutMs);
    setNextDirectory(item.getRawEntry());
    String payload = item.getPayload();

    // check for error
    String error = getError(payload);
    if (error != null) {
      throw new SorobanException("Partner failed with error: " + error);
    }
    return payload;
  }

  public Completable sendWithSender(SorobanPayloadable message, PaymentCode paymentCodePartner)
      throws Exception {
    checkExit(paymentCodePartner);
    return rpcSession.withSorobanClient(
        sorobanClient -> {
          return Completable.fromSingle(
              computeEndpointWithSender(paymentCodePartner)
                  .sendSingle(sorobanClient, message)
                  .map(
                      messageWithSender -> {
                        setNextDirectory(messageWithSender.getRawEntry());
                        return messageWithSender;
                      }));
        });
  }

  public Completable send(SorobanPayloadable payload, PaymentCode paymentCodePartner)
      throws Exception {
    checkExit(paymentCodePartner);
    return Completable.fromSingle(
        rpcSession.withSorobanClient(
            sorobanClient ->
                computeEndpoint(paymentCodePartner)
                    .sendSingle(sorobanClient, payload)
                    .map(
                        req -> {
                          setNextDirectory(req.getRawEntry());
                          return req;
                        })));
  }

  private void checkExit(PaymentCode paymentCodePartner) throws Exception {
    if (exit) {
      sendError("Canceled by user", paymentCodePartner).subscribe();
      throw new SorobanException("Canceled by user");
    }
  }

  public Completable sendError(String message, PaymentCode paymentCodePartner) throws Exception {
    SorobanPayloadable payload = () -> ERROR_PREFIX + message;
    return send(payload, paymentCodePartner);
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

  private String getError(String payload) {
    if (payload != null && payload.startsWith(ERROR_PREFIX)) {
      return payload.substring(ERROR_PREFIX.length());
    }
    return null;
  }

  public void close() {
    exit = true;
    rpcSession.exit();
  }
}
