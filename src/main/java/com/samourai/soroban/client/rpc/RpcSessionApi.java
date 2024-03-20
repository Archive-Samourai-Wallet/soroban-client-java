package com.samourai.soroban.client.rpc;

import com.samourai.wallet.bip47.rpc.PaymentCode;

public class RpcSessionApi {
  protected final RpcSession rpcSession;

  public RpcSessionApi(RpcSession rpcSession) {
    this.rpcSession = rpcSession;
  }

  //

  public RpcSession getRpcSession() {
    return rpcSession;
  }

  public PaymentCode getPaymentCode() {
    return rpcSession.getRpcWallet().getBip47Account().getPaymentCode();
  }
}
