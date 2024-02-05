package com.samourai.soroban.client.endpoint.controller;

import com.samourai.soroban.client.SorobanPayloadable;
import com.samourai.soroban.client.endpoint.meta.typed.SorobanEndpointTyped;
import com.samourai.soroban.client.endpoint.meta.typed.SorobanItemTyped;
import com.samourai.soroban.client.exception.SorobanException;
import com.samourai.soroban.client.rpc.RpcSession;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import java.util.Collection;

public abstract class SorobanControllerTyped
    extends SorobanController<SorobanItemTyped, SorobanEndpointTyped> {

  public SorobanControllerTyped(
      int startDelayMs, String logId, RpcSession rpcSession, SorobanEndpointTyped endpoint) {
    super(startDelayMs, logId, rpcSession, endpoint);
  }

  public SorobanControllerTyped(
      int loopDelayMs,
      int startDelayMs,
      String logId,
      RpcSession rpcSession,
      SorobanEndpointTyped endpoint) {
    super(loopDelayMs, startDelayMs, logId, rpcSession, endpoint);
  }

  @Override
  protected Collection<SorobanItemTyped> fetch() throws Exception {
    return asyncUtil.blockingGet(
        rpcSession.withSorobanClient(
            rpcClient -> endpoint.getList(rpcClient, f -> f.distinctByUniqueIdWithLastNonce())));
  }

  @Override
  protected String computeUniqueId(SorobanItemTyped message) {
    return message.getUniqueId();
  }

  @Override
  protected Long computeNonce(SorobanItemTyped request) {
    return request.getMetaNonce();
  }

  @Override
  protected void onRequestNew(SorobanItemTyped request, String key) throws Exception {
    SorobanPayloadable reply = computeReplyOnRequestNew(request, key);
    if (reply != null) {
      logDebug("NEW REPLY " + reply.getClass().getSimpleName() + " => " + request.getType());
      sendReply(request, reply);
    } else {
      logDebug("NO REPLY => " + request.getType());
    }
  }

  @Override
  protected void onRequestExisting(SorobanItemTyped request, String key) throws Exception {
    super.onRequestExisting(request, key);
    SorobanPayloadable reply = computeReplyOnRequestExisting(request, key);
    if (reply != null) {
      logDebug("REPLY (existing) " + reply.getClass().getSimpleName());
      sendReply(request, reply);
    } else {
      logDebug("NO REPLY (existing) for " + request.getType());
    }
  }

  @Override
  protected void onRequestIgnored(SorobanItemTyped request, String key) throws Exception {
    super.onRequestIgnored(request, key);
    SorobanPayloadable reply = computeReplyOnRequestIgnored(request, key);
    if (reply != null) {
      logDebug("REPLY (ignored) " + reply.getClass().getSimpleName() + " => " + request.getType());
      sendReply(request, reply);
    } else {
      logDebug("NO REPLY (ignored) for " + request.getType());
    }
  }

  protected abstract SorobanPayloadable computeReplyOnRequestExisting(
      SorobanItemTyped request, String key) throws Exception;

  protected SorobanPayloadable computeReplyOnRequestIgnored(SorobanItemTyped request, String key)
      throws Exception {
    return null;
  }

  protected abstract SorobanPayloadable computeReplyOnRequestNew(
      SorobanItemTyped request, String key) throws Exception;

  protected void sendReply(SorobanItemTyped request, SorobanPayloadable response) throws Exception {
    // reply to request
    PaymentCode paymentCodePartner = request.getMetaSender();
    if (paymentCodePartner == null) {
      throw new SorobanException("missing request.metadata.sender for sendReply()");
    }
    Bip47Encrypter encrypter = rpcSession.getRpcWallet().getBip47Encrypter();
    rpcSession
        .withSorobanClient(
            sorobanClient ->
                endpoint.getEndpointReply(request, encrypter).send(sorobanClient, response))
        .subscribe();
  }
}
