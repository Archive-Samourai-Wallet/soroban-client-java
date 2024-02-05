package com.samourai.soroban.client.dialog;

import com.google.common.base.Charsets;
import com.samourai.soroban.client.SorobanPayloadable;
import com.samourai.soroban.client.exception.SorobanException;
import com.samourai.soroban.client.meeting.SorobanMessageWithSender;
import com.samourai.soroban.client.rpc.RpcSession;
import com.samourai.wallet.bip47.rpc.Bip47Partner;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.security.MessageDigest;
import java.util.function.Consumer;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated // TODO
public class RpcDialog {
  private static final Logger log = LoggerFactory.getLogger(RpcDialog.class);
  private static final int LOOP_FREQUENCY_MS = 1000;
  private static final String ERROR_PREFIX = "ERROR:";

  private RpcSession rpcSession;
  private Consumer<String> onEncryptedPayload;

  private String nextDirectory;
  private boolean exit;

  public RpcDialog(String directory, RpcSession rpcSession) throws Exception {
    this.rpcSession = rpcSession;
    this.onEncryptedPayload =
        payload -> {
          try {
            setNextDirectory(payload);
          } catch (Exception e) {
            log.error("", e);
          }
        };

    setNextDirectory(directory);
    this.exit = false;
  }

  public Single<SorobanMessageWithSender> receiveWithSender() {
    return null; // TODO
    /*return rpcSession.directoryValueWait(
    nextDirectory,
    (sorobanClient, value) -> {
      // read paymentCode from sender
      SorobanMessageWithSender messageWithSender = SorobanMessageWithSender.parse(value);
      String encryptedPayload = messageWithSender.getPayload();
      String sender = messageWithSender.getSender();
      PaymentCode paymentCodePartner = new PaymentCode(sender);

      // decrypt
      onEncryptedPayload.accept(value);
      if (true) throw new RuntimeException("TODO zl !!!");
      String payload = "TODO zl !!!";
      // TODO String payload = sorobanClient.decrypt(encryptedPayload, paymentCodePartner);

      // return clear object
      return new SorobanMessageWithSender(sender, payload);
    });*/
  }

  public Single<String> receive(final PaymentCode paymentCodePartner, int timeoutMs)
      throws Exception {
    Bip47Partner bip47Partner =
        rpcSession.getRpcWallet().getBip47Partner(paymentCodePartner, false);
    // RpcSessionPartnerApi partnerApi =
    //   new RpcSessionPartnerApi(rpcSession, bip47Partner, requestId -> nextDirectory);
    return null; // TODO zl
    /*return partnerApi
    .loopUntilReply("RpcDialog", LOOP_FREQUENCY_MS) // requestId is not used
    .map(untypedPayload -> untypedPayload.getPayload())
    .timeout(timeoutMs, TimeUnit.MILLISECONDS);*/
  }

  public Completable sendWithSender(SorobanPayloadable message, PaymentCode paymentCodePartner)
      throws Exception {
    return null; // TODO zl
    /*
    checkExit(paymentCodePartner);
    return rpcSession.withSorobanClient(
        sorobanClient -> {
          // encrypt
          String encryptedPayload = sorobanClient.encrypt(message.toPayload(), paymentCodePartner);
          onEncryptedPayload.accept(encryptedPayload);

          // wrap with clear sender
          PaymentCode paymentCodeMine = rpcSession.getRpcWallet().getBip47Account().getPaymentCode();
          String payloadWithSender =
              SorobanMessageWithSender.toPayload(paymentCodeMine.toString(), encryptedPayload);
          return sorobanClient
              .getRpcClient()
              .directoryAdd(nextDirectory, payloadWithSender, RpcMode.NORMAL);
        });*/
  }

  public Completable send(SorobanPayloadable message, PaymentCode paymentCodePartner)
      throws Exception {
    return send(message.toPayload(), paymentCodePartner);
  }

  private Completable send(String payload, PaymentCode paymentCodePartner) throws Exception {
    return null; // TODO zl
    /*checkExit(paymentCodePartner);
    return rpcSession.withSorobanClient(
        sorobanClient -> {
          String encryptedPayload = sorobanClient.encrypt(payload, paymentCodePartner);
          onEncryptedPayload.accept(encryptedPayload);
          return sorobanClient
              .getRpcClient()
              .directoryAdd(nextDirectory, encryptedPayload, RpcMode.NORMAL);
        });*/
  }

  private void checkExit(PaymentCode paymentCodePartner) throws Exception {
    if (exit) {
      sendError("Canceled by user", paymentCodePartner).subscribe();
      throw new SorobanException("Canceled by user");
    }
  }

  public Single<String> sendError(String message, PaymentCode paymentCodePartner) throws Exception {
    String payload = ERROR_PREFIX + message;
    return send(payload, paymentCodePartner).toSingle(() -> payload);
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
  }
}
