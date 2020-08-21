package com.samourai.soroban.client.dialog;

import com.samourai.soroban.client.rpc.RpcClient;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcDialog {
  private static final Logger log = LoggerFactory.getLogger(RpcDialog.class);

  private RpcClient rpc;
  private Box box;
  private String nextDirectory;

  private RpcDialog(RpcClient rpc, Box box) throws Exception {
    this.rpc = rpc;
    this.box = box;
    this.nextDirectory =
        box.initialDirectory(); // TODO ZL // RpcClient.encodeDirectory(user.sharedSecret(box));
  }

  public static RpcDialog initiator(
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
    String initialDirectory =
        user.getMeeetingAddressSend(paymentCodePartner, params).getBech32AsString();
    if (log.isDebugEnabled()) {
      log.debug("Connecting initiator => " + initialDirectory);
    }

    Box box = user.box(initialDirectory);
    return new RpcDialog(rpc, box);
  }

  public static RpcDialog contributor(
      RpcClient rpc, User user, PaymentCode paymentCodePartner, NetworkParameters params)
      throws Exception {
    // TODO ZL
    /*SegwitAddress meetingAddress = user.getMeeetingAddressAsCounterparty(paymentCodeInitiator, params);
    String directoryName = RpcClient.encodeDirectory(meetingAddress.getBech32AsString());

    // get initiator signature
    String initiatorSignature = rpc.waitAndRemove(directoryName, 10);
    if (!"initiatorSignature".equals(initiatorSignature)) {
      throw new Exception("Invalid initiator signature"); // TODO ZL signature
    }
    if (log.isDebugEnabled()) {
      log.debug("initiator found: "+initiatorSignature);
    }

    String privateDirectory = String.format("%s.%s", directoryName, meetingAddress.getECKey().getPubKey());
    privateDirectory = RpcClient.encodeDirectory(privateDirectory);

    // send signature
    rpc.directoryAdd(privateDirectory, "contributorSignature", "default"); // TODO ZL signature
    if (log.isDebugEnabled()) {
      log.debug("publickey sent");
    }

    // instanciate
    byte[] pubKey = Hex.decode(initiatorPublicKey);
    return new RpcDialog(rpc, user, pubKey);*/

    String initialDirectory =
        user.getMeeetingAddressReceive(paymentCodePartner, params).getBech32AsString();
    if (log.isDebugEnabled()) {
      log.debug("Connecting contributor => " + initialDirectory);
    }
    Box box = user.box(initialDirectory);
    return new RpcDialog(rpc, box);
  }

  public String receive() throws Exception {
    String payload = rpc.waitAndRemove(nextDirectory, 10);
    String message = box.decrypt(payload);
    if (log.isDebugEnabled()) {
      log.debug("(" + nextDirectory + ") <= " + message);
    }
    nextDirectory = RpcClient.encodeDirectory(payload);
    return message;
  }

  public void send(String payload) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("(" + nextDirectory + ") => " + payload);
    }
    payload = box.encrypt(payload);
    rpc.directoryAdd(nextDirectory, payload, "short");
    nextDirectory = RpcClient.encodeDirectory(payload);
  }
}
