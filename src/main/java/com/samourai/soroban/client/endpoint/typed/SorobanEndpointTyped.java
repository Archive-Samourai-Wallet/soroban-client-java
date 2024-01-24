package com.samourai.soroban.client.endpoint.typed;

import com.samourai.soroban.client.SorobanPayload;
import com.samourai.soroban.client.SorobanPayloadable;
import com.samourai.soroban.client.endpoint.AbstractSorobanEndpoint;
import com.samourai.soroban.client.endpoint.SorobanApp;
import com.samourai.soroban.client.rpc.RpcMode;
import com.samourai.soroban.client.wrapper.SorobanWrapper;

public class SorobanEndpointTyped<T extends SorobanPayloadable>
    extends AbstractSorobanEndpoint<T, SorobanPayloadTyped<T>> {

  public SorobanEndpointTyped(
      SorobanApp app, String path, RpcMode rpcMode, SorobanWrapper... wrappers) {
    super(app, path, rpcMode, wrappers);
  }

  @Override
  protected SorobanPayloadTyped adaptOnSend(String sender, SorobanPayloadable payload)
      throws Exception {
    return new SorobanPayloadTyped(sender, payload);
  }

  @Override
  protected SorobanPayloadTyped adaptOnReceive(SorobanPayload entry) {
    return new SorobanPayloadTyped(entry);
  }
}
