package com.samourai.soroban.client.endpoint.string;

import com.samourai.soroban.client.SorobanPayload;
import com.samourai.soroban.client.endpoint.AbstractSorobanEndpoint;
import com.samourai.soroban.client.endpoint.SorobanApp;
import com.samourai.soroban.client.endpoint.SorobanPayloadImpl;
import com.samourai.soroban.client.rpc.RpcMode;
import com.samourai.soroban.client.wrapper.SorobanWrapper;

public class SorobanEndpointString extends AbstractSorobanEndpoint<String, SorobanPayload> {

  public SorobanEndpointString(
      SorobanApp app, String path, RpcMode rpcMode, SorobanWrapper... wrappers) {
    super(app, path, rpcMode, wrappers);
  }

  @Override
  protected SorobanPayload adaptOnSend(String sender, String payload) {
    return new SorobanPayloadImpl(sender, payload);
  }

  @Override
  protected SorobanPayload adaptOnReceive(SorobanPayload entry) {
    return entry;
  }
}
