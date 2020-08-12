package com.samourai.soroban.client;

import com.samourai.soroban.client.rpc.RpcClient;

public final class SorobanClient {

  public static void main(String[] args) throws Exception {

    // parse cli
    String torProxy = "127.0.0.1";
    int torPort = 9050;
    String torUrl = "http://sorob4sg7yiopktgz4eom7hl5mcodr6quvhmdpljl5qqhmt6po7oebid.onion";
    String role = "contributor";
    String directoryName = "samourai.soroban.private";
    int numIter = 3;

    if (args.length > 0) {
      torProxy = args[0];
    }
    if (args.length > 1) {
      torPort = Integer.parseInt(args[1]);
    }
    if (args.length > 2) {
      torUrl = args[2];
    }
    if (args.length > 3) {
      role = args[3];
    }
    if (args.length > 4) {
      directoryName = args[4];
    }
    if (args.length > 5) {
      numIter = Integer.parseInt(args[5]);
    }

    RpcClient rpc = new RpcClient(torProxy, torPort, torUrl);
    try {
      while (true) {
        if ("initiator".equals(role.toLowerCase())) {
          SorobanPingPong.initiator(rpc, directoryName, numIter);
        } else {
          SorobanPingPong.contributor(rpc, directoryName, numIter);
        }
      }
    } finally {
      rpc.close();
    }
  }
}
