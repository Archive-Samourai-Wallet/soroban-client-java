package com.samourai.soroban.client.endpoint.meta;

import com.samourai.soroban.client.endpoint.SorobanApp;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaEncryptWithSender;
import com.samourai.soroban.client.endpoint.wrapper.SorobanWrapper;
import com.samourai.soroban.client.rpc.RpcMode;
import java.util.List;

public class SorobanEndpointMetaString
    extends AbstractSorobanEndpointMeta<SorobanItem, SorobanList<SorobanItem>> {

  public SorobanEndpointMetaString(
      SorobanApp app, String path, RpcMode rpcMode, SorobanWrapper[] wrappers) {
    super(app, path, rpcMode, wrappers);
  }

  @Override
  protected SorobanItem newEntry(SorobanEntryMeta entryMeta, String rawEntry) {
    return new SorobanItem(entryMeta.getPayload(), entryMeta.getMetadata(), rawEntry, this);
  }

  @Override
  protected SorobanList<SorobanItem> newList(List items) {
    return new SorobanList(items);
  }

  @Override
  public SorobanEndpointMetaString getEndpointReply(SorobanItem request) {
    return new SorobanEndpointMetaString(
        getApp(),
        getPathReply(request),
        RpcMode.SHORT,
        new SorobanWrapper[] {new SorobanWrapperMetaEncryptWithSender(request.getMetaSender())});
  }
}
