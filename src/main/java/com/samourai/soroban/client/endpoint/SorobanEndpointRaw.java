package com.samourai.soroban.client.endpoint;

import com.samourai.soroban.client.endpoint.wrapper.SorobanWrapperString;
import com.samourai.soroban.client.rpc.RpcMode;
import com.samourai.wallet.util.Pair;
import java.util.LinkedList;
import java.util.List;

/**
 * This endpoint uses raw String as payload.
 *
 * <p>This endpoint has limited features:<br>
 * - only support SorobanWrapperString[]<br>
 * - .remove() is NOT supported when using SorobanWrapperEncrypt, use .removeRaw() instead
 */
public class SorobanEndpointRaw
    extends AbstractSorobanEndpoint<String, List<String>, String, Void> {
  public SorobanEndpointRaw(
      SorobanApp app, String path, RpcMode rpcMode, SorobanWrapperString[] wrappers) {
    super(app, path, rpcMode, wrappers);
  }

  @Override
  protected String entryToRaw(Pair<String, Void> entry) throws Exception {
    return entry.getLeft();
  }

  @Override
  protected Pair<String, Void> rawToEntry(String rawEntry) throws Exception {
    return Pair.of(rawEntry, null);
  }

  @Override
  protected List<String> newList(List<String> items) {
    return new LinkedList<>(items);
  }

  @Override
  protected Pair<String, Void> newEntry(String payload) throws Exception {
    return Pair.of(payload, null);
  }

  @Override
  protected String newEntry(Pair<String, Void> entry, String rawEntry) {
    return entry.getLeft();
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
