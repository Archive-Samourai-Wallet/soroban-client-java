package com.samourai.soroban.client;

import com.samourai.soroban.client.dialog.RpcDialog;
import com.samourai.soroban.client.rpc.RpcClient;
import java.util.Date;
import org.apache.http.client.utils.DateUtils;
import org.bitcoinj.core.ECKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanPingPong {
  private static final Logger log = LoggerFactory.getLogger(SorobanPingPong.class);

  public static void initiator(RpcClient rpc, String directoryName, ECKey pkey, int numIter)
      throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("Connecting initiator...");
    }
    RpcDialog dialog = RpcDialog.initiator(rpc, directoryName, pkey);

    if (log.isDebugEnabled()) {
      log.debug("Starting echange loop...");
    }
    int counter = 1;
    while (numIter > 0) {
      // request
      String payload = String.format("Ping %d %s", counter, DateUtils.formatDate(new Date()));
      dialog.send(payload);

      // response
      String message = dialog.receive();

      counter += 1;
      numIter -= 1;

      if (log.isDebugEnabled()) {
        log.debug("initiator: #" + counter + " done");
      }
    }
  }

  public static void contributor(RpcClient rpc, String directoryName, ECKey pkey, int numIter)
      throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("Connecting contributor...");
    }
    RpcDialog dialog = RpcDialog.contributor(rpc, directoryName, pkey);

    if (log.isDebugEnabled()) {
      log.debug("Starting echange loop...");
    }
    int counter = 1;
    while (numIter > 0) {
      // query
      String message = dialog.receive();

      // response
      String payload = String.format("Pong %d %s", counter, DateUtils.formatDate(new Date()));
      dialog.send(payload);

      counter += 1;
      numIter -= 1;

      if (log.isDebugEnabled()) {
        log.debug("contributor: #" + counter + " done");
      }
    }
  }
}
