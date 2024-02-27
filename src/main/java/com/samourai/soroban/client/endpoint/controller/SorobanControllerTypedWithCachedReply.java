package com.samourai.soroban.client.endpoint.controller;

import com.samourai.soroban.client.endpoint.meta.typed.SorobanEndpointTyped;
import com.samourai.soroban.client.endpoint.meta.typed.SorobanItemTyped;
import com.samourai.soroban.client.rpc.RpcSession;
import com.samourai.wallet.sorobanClient.SorobanPayloadable;
import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SorobanControllerTypedWithCachedReply extends SorobanControllerTyped {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private Map<String, SorobanPayloadable> replyByRequestId; // reply by request.uniqueId

  public SorobanControllerTypedWithCachedReply(
      int startDelayMs, String logId, RpcSession rpcSession, SorobanEndpointTyped endpoint) {
    this(endpoint.getPollingFrequencyMs(), startDelayMs, logId, rpcSession, endpoint);
  }

  public SorobanControllerTypedWithCachedReply(
      int loopDelayMs,
      int startDelayMs,
      String logId,
      RpcSession rpcSession,
      SorobanEndpointTyped endpoint) {
    super(loopDelayMs, startDelayMs, logId, rpcSession, endpoint);
    this.replyByRequestId = new LinkedHashMap<>();
  }

  @Override
  protected void onExpiring(String key) throws Exception {
    super.onExpiring(key);
    replyByRequestId.remove(key);
  }

  // return null for no reply
  protected abstract SorobanPayloadable computeReplyOnRequestNewForCaching(
      SorobanItemTyped request, String key) throws Exception;

  @Override
  protected boolean isRequestIgnored(SorobanItemTyped request, String uniqueId) {
    Long requestNonce = request.getMetaNonce();
    if (requestNonce == null) {
      log.warn("REQUEST ignored (missing metadata.nonce): " + request.toString());
      return true;
    }
    return false;
  }

  @Override
  protected final SorobanPayloadable computeReplyOnRequestNew(SorobanItemTyped request, String key)
      throws Exception {
    // compute new reply
    SorobanPayloadable reply = computeReplyOnRequestNewForCaching(request, key);
    this.replyByRequestId.put(key, reply);
    return reply;
  }

  @Override
  protected SorobanPayloadable computeReplyOnRequestExisting(SorobanItemTyped request, String key)
      throws Exception {
    // re-send cached reply
    return this.replyByRequestId.get(key);
  }
}
