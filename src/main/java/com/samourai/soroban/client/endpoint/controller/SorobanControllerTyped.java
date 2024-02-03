package com.samourai.soroban.client.endpoint.controller;

import com.samourai.soroban.client.SorobanPayloadable;
import com.samourai.soroban.client.endpoint.meta.typed.SorobanEndpointTyped;
import com.samourai.soroban.client.endpoint.meta.typed.SorobanItemTyped;
import com.samourai.soroban.client.rpc.RpcSession;
import java.util.Collection;

public abstract class SorobanControllerTyped
    extends SorobanController<SorobanItemTyped, SorobanEndpointTyped> {

  public SorobanControllerTyped(
      int LOOP_DELAY, String logId, RpcSession rpcSession, SorobanEndpointTyped endpoint) {
    super(LOOP_DELAY, logId, rpcSession, endpoint);
  }

  @Override
  protected Collection<SorobanItemTyped> fetch() throws Exception {
    return asyncUtil.blockingGet(
        rpcSession.withSorobanClient(rpcClient -> endpoint.getList(rpcClient)));
  }

  @Override
  protected String computeUniqueId(SorobanItemTyped message) {
    return message.getUniqueId();
  }

  protected void sendReply(SorobanItemTyped request, SorobanPayloadable response) throws Exception {
    // reply to request
    rpcSession
        .withSorobanClient(
            sorobanClient -> endpoint.getEndpointReply(request).send(sorobanClient, response))
        .subscribe();
  }
}
