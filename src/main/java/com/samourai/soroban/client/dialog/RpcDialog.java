package com.samourai.soroban.client.dialog;

import com.google.common.base.Charsets;
import com.samourai.soroban.client.rpc.RpcClient;
import com.samourai.wallet.soroban.client.SorobanMessage;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import java.security.MessageDigest;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcDialog {
  private static final Logger log = LoggerFactory.getLogger(RpcDialog.class);

  private RpcClient rpc;
  private Box box;
  private String nextDirectory;

  public RpcDialog(RpcClient rpc, User user, String directory) throws Exception {
    this.rpc = rpc;
    this.box = user.box();
    setNextDirectory(directory); // TODO ZL // RpcClient.encodeDirectory(user.sharedSecret(box));
  }

  public RpcDialog(RpcClient rpc, User user, SorobanMessage sorobanMessage) throws Exception {
    this(rpc, user, sorobanMessage.toPayload());
  }

  /*public static RpcDialog dialog(
        RpcClient rpc, User user, PaymentCode paymentCodePartner, NetworkParameters params)
        throws Exception {
  // TODO ZL
  /*SegwitAddress meetingAddress = user.getMeeetingAddressAsInitiator(paymentCodeCounterparty, params);
  String directoryName = RpcClient.encodeDirectory(meetingAddress.getBech32AsString());

  // send signature
  //rpc.directoryAdd(directoryName, user.publicKey(), "long");
  rpc.directoryAdd(directoryName, "initiatorSignature", "long"); // TODO ZL signature
  if (log.isDebugEnabled()) {
    log.debug("initiator is ready");
  }

  String privateDirectory = String.format("%s.%s", directoryName, meetingAddress.getECKey().getPubKey());
  privateDirectory = RpcClient.encodeDirectory(privateDirectory);

  // wait for contributor
  String contributorSignature = rpc.waitAndRemove(privateDirectory, 100);
  if (!"contributorSignature".equals(contributorSignature)) {
    throw new Exception("Invalid contributor signature"); // TODO ZL signature
  }
  if (log.isDebugEnabled()) {
    log.debug("contributor connected: " + contributorSignature);
  }

  // instanciate
  byte[] pubKey = Hex.decode(candidatePublicKey);
  return new RpcDialog(rpc, user, pubKey);*/

  // instanciate
  // byte[] pubKey = user.getMeeetingAddressSend(paymentCodePartner,
  // params).getECKey().getPubKey();
  /*String initialDirectory =
            user.getMeeetingAddressSend(paymentCodePartner, params).getBech32AsString();
    if (log.isDebugEnabled()) {
      log.debug("Connecting initiator => " + initialDirectory);
    }

    return new RpcDialog(rpc, user, initialDirectory);
  }*/

  public Observable<String> receive(long timeoutMs) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("watching: " + nextDirectory);
    }
    return rpc.waitAndRemove(nextDirectory, timeoutMs)
        .map(
            new Function<String, String>() {
              @Override
              public String apply(String payload) throws Exception {
                String message = box.decrypt(payload);
                if (log.isDebugEnabled()) {
                  log.debug("(" + nextDirectory + ") <= " + message);
                }
                setNextDirectory(payload);
                return message;
              }
            });
  }

  public Observable send(SorobanMessage message) throws Exception {
    return send(message.toPayload());
  }

  private Observable send(String payload) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("(" + nextDirectory + ") => " + payload);
    }
    payload = box.encrypt(payload);

    final String myPayload = payload;
    return rpc.directoryAdd(nextDirectory, payload, "short")
        .map(
            new Function() {
              @Override
              public Object apply(Object o) throws Exception {
                setNextDirectory(myPayload);
                return o;
              }
            });
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
    if (log.isDebugEnabled()) {
      log.debug("nextDirectory: " + this.nextDirectory);
    }
  }
}
