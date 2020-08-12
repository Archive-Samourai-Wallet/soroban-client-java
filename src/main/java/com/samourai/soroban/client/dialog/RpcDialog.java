package com.samourai.soroban.client.dialog;

import com.samourai.soroban.client.rpc.RpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcDialog {
  private static final Logger log = LoggerFactory.getLogger(RpcDialog.class);

  private RpcClient rpc;
  private Box box;
  private String nextDirectory;

  private RpcDialog(RpcClient rpc, User user, String pubKey) throws Exception {
    this.rpc = rpc;
    this.box = user.box(pubKey);
    this.nextDirectory = RpcClient.encodeDirectory(user.sharedSecret(box));
  }

  public static RpcDialog initiator(RpcClient rpc, String directoryName) throws Exception {
    directoryName = RpcClient.encodeDirectory(directoryName);

    // send public key
    User user = new User();
    rpc.directoryAdd(directoryName, user.publicKey(), "long");
    if (log.isDebugEnabled()) {
      log.debug("initiator is ready");
    }

    String privateDirectory = String.format("%s.%s", directoryName, user.publicKey());
    privateDirectory = RpcClient.encodeDirectory(privateDirectory);

    // wait for contributor
    String candidatePublicKey = rpc.waitAndRemove(privateDirectory, 100);
    if (log.isDebugEnabled()) {
      log.debug("contributor connected: " + candidatePublicKey);
    }

    // instanciate
    return new RpcDialog(rpc, user, candidatePublicKey);
  }

  public static RpcDialog contributor(RpcClient rpc, String directoryName) throws Exception {
    directoryName = RpcClient.encodeDirectory(directoryName);

    // get initiator public key
    String initiatorPublicKey = rpc.waitAndRemove(directoryName, 10);
    if (log.isDebugEnabled()) {
      log.debug("initiator found");
    }

    String privateDirectory = String.format("%s.%s", directoryName, initiatorPublicKey);
    privateDirectory = RpcClient.encodeDirectory(privateDirectory);

    // send public key
    User user = new User();
    rpc.directoryAdd(privateDirectory, user.publicKey(), "default");
    if (log.isDebugEnabled()) {
      log.debug("publickey sent");
    }

    // instanciate
    return new RpcDialog(rpc, user, initiatorPublicKey);
  }

  public String receive() throws Exception {
    String payload = rpc.waitAndRemove(nextDirectory, 10);
    nextDirectory = RpcClient.encodeDirectory(payload);

    String message = box.decrypt(payload);
    if (log.isDebugEnabled()) {
      log.debug("Received: " + message);
    }
    return message;
  }

  public void send(String payload) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("Sending: " + payload);
    }
    payload = box.encrypt(payload);
    rpc.directoryAdd(nextDirectory, payload, "short");
    nextDirectory = RpcClient.encodeDirectory(payload);
  }
}
