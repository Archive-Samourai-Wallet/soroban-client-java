package com.samourai.soroban.client.rpc;

import com.samourai.soroban.client.SorobanClient;
import com.samourai.soroban.client.SorobanPayload;
import com.samourai.wallet.util.Util;
import io.reactivex.Completable;

public class RpcSessionApi {
  protected final RpcSession rpcSession;

  public RpcSessionApi(RpcSession rpcSession) {
    this.rpcSession = rpcSession;
  }

  // overridable
  public String getRequestId(String requestPayload) {
    return Util.sha256Hex(requestPayload);
  }

  //

  public Completable sendClearSignedWithSender(
      SorobanPayload sorobanPayload, String directory, SorobanClient sorobanClient)
      throws Exception {
    String signedPayload = sorobanClient.signWithSender(sorobanPayload.toPayload());
    return sorobanClient.getRpcClient().directoryAdd(directory, signedPayload, RpcMode.SHORT);
  }

  public RpcSession getRpcSession() {
    return rpcSession;
  }
}
