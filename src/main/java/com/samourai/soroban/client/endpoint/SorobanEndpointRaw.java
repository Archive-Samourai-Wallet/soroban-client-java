package com.samourai.soroban.client.endpoint;

import com.samourai.soroban.client.endpoint.wrapper.SorobanWrapperString;
import com.samourai.soroban.client.rpc.RpcMode;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import java.util.LinkedList;
import java.util.List;

public class SorobanEndpointRaw extends AbstractSorobanEndpoint<String, List<String>, String> {
  public SorobanEndpointRaw(
      SorobanApp app, String path, RpcMode rpcMode, SorobanWrapperString[] wrappers) {
    super(app, path, rpcMode, wrappers);
  }

  @Override
  protected String toPayload(String entry) throws Exception {
    return entry;
  }

  @Override
  protected String readEntry(Bip47Encrypter encrypter, String entry) {
    return entry;
  }

  @Override
  protected List<String> newList(List<String> items) {
    return new LinkedList<>(items);
  }

  @Override
  protected String newEntry(String entry) {
    return entry;
  }

  @Override
  protected String newEntry(String entry, String rawEntry) {
    return entry;
  }

  @Override
  protected String getRawEntry(String entry) {
    return entry;
  }

  @Override
  public String computeUniqueId(String entry) {
    return entry;
  }

  @Override
  public SorobanEndpoint getEndpointReply(String request) {
    return new SorobanEndpointRaw(
        getApp(), getPath(), RpcMode.SHORT, new SorobanWrapperString[] {});
  }
}
