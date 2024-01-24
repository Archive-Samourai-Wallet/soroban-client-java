package com.samourai.soroban.client.rpc;

import com.samourai.wallet.util.Util;

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

  public RpcSession getRpcSession() {
    return rpcSession;
  }
}
