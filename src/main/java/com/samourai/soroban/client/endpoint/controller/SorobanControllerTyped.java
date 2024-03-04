package com.samourai.soroban.client.endpoint.controller;

import com.samourai.soroban.client.endpoint.meta.typed.SorobanEndpointTyped;
import com.samourai.soroban.client.endpoint.meta.typed.SorobanItemTyped;
import com.samourai.soroban.client.exception.SorobanException;
import com.samourai.soroban.client.rpc.RpcSession;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.sorobanClient.SorobanPayloadable;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SorobanControllerTyped
    extends SorobanController<SorobanItemTyped, SorobanEndpointTyped> {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
  protected void onRequest(SorobanItemTyped request) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug(
          "(<) "
              + request.getClass().getSimpleName()
              + " "
              + endpoint.getDir()
              + " sender="
              + request.getMetaSender());
    }
    SorobanPayloadable reply = computeReply(request);
    if (reply != null) {
      sendReply(request, reply);
    }
  }

  protected abstract SorobanPayloadable computeReply(SorobanItemTyped request) throws Exception;

  protected void sendReply(SorobanItemTyped request, SorobanPayloadable response) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug(
          "(>) "
              + response.getClass().getSimpleName()
              + " "
              + endpoint.getDir()
              + " sender="
              + request.getMetaSender());
    }
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
