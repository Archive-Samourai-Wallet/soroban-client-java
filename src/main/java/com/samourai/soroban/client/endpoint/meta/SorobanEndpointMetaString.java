package com.samourai.soroban.client.endpoint.meta;

import com.samourai.soroban.client.endpoint.SorobanApp;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaEncryptWithSender;
import com.samourai.soroban.client.endpoint.wrapper.SorobanWrapper;
import com.samourai.soroban.client.rpc.RpcMode;
import com.samourai.wallet.util.Pair;
import java.util.List;

/**
 * This endpoint sends & receives String payloads, with full Soroban features support. Payloads are
 * wrapped with metadatas.
 */
public class SorobanEndpointMetaString
    extends AbstractSorobanEndpointMeta<SorobanItem, SorobanList<SorobanItem>, String> {

  public SorobanEndpointMetaString(
      SorobanApp app, String path, RpcMode rpcMode, SorobanWrapper[] wrappers) {
    super(app, path, rpcMode, wrappers);
  }

  @Override
  protected Pair<String, SorobanMetadata> newEntry(String payload) throws Exception {
    return Pair.of(payload, new SorobanMetadataImpl());
  }

  @Override
  protected SorobanItem newEntry(Pair<String, SorobanMetadata> entry, String rawEntry) {
    return new SorobanItem(entry.getLeft(), entry.getRight(), rawEntry, this);
  }

  @Override
  protected SorobanList<SorobanItem> newList(List items) {
    return new SorobanList(items);
  }

  @Override
  public SorobanEndpointMetaString getEndpointReply(SorobanItem request) {
    if (request.getMetaSender() == null) {
      throw new RuntimeException(
          "getEndpointReply() failed: missing metadata.sender, please enable SorobanWrapperMetaSender");
    }
    return new SorobanEndpointMetaString(
        getApp(),
        getPathReply(request),
        RpcMode.SHORT,
        new SorobanWrapper[] {new SorobanWrapperMetaEncryptWithSender(request.getMetaSender())});
  }
}
