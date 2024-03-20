package com.samourai.soroban.client.rpc;

import com.samourai.wallet.bip47.rpc.Bip47Partner;

public class RpcSessionPartnerApi extends RpcSessionApi {

  protected final Bip47Partner bip47Partner;

  public RpcSessionPartnerApi(RpcSession rpcSession, Bip47Partner bip47Partner) {
    super(rpcSession);
    this.bip47Partner = bip47Partner;
  }

  //

  /*
  //TODO
  public Single<String> sendReplyEncrypted(
      SorobanPayloadable sorobanPayload, String requestId, SorobanClient sorobanClient)
      throws Exception {
    PaymentCode paymentCodePartner = bip47Partner.getPaymentCodePartner();
    String sorobanPayloadStr = sorobanPayload.toPayload();
    String directory = getDirectoryReply(requestId);
    if (log.isDebugEnabled()) {
      log.debug(
          " -> "
              + " ["
              + sorobanPayload.getClass().getSimpleName()
              + "] "
              + RpcClient.shortDirectory(directory)
              + " ("
              + Util.maskString(paymentCodePartner.toString())
              + "): "
              + sorobanPayloadStr);
    }
    String encryptedPayload = sorobanClient.encrypt(sorobanPayloadStr, paymentCodePartner);
    return sorobanClient
        .getRpcClient()
        .directoryAdd(directory, encryptedPayload, RpcMode.SHORT)
        .toSingle(() -> getRequestId(sorobanPayloadStr));
  }*/

  //

  /*public Single<SorobanPayloadTyped> loopUntilReply(String requestId, long loopFrequencyMs) {
    return loopUntilReply(sorobanClient -> Single.just(requestId), loopFrequencyMs);
  }

  public <T extends SorobanPayloadable> Single<T> loopUntilReplyTyped(
      String requestId, long loopFrequencyMs, Class<T> typeReply) {
    return loopUntilReply(requestId, loopFrequencyMs)
        .map(untypedPayload -> untypedPayload.read(typeReply));
  }

  public Single<AckResponse> loopUntilReplyAck(String requestId, long loopFrequencyMs) {
    return loopUntilReplyAck(sorobanClient -> Single.just(requestId), loopFrequencyMs);
  }

  public Single<AckResponse> loopUntilReplyAck(
      CallbackWithArg<SorobanClient, Single<String>> sendRequestOrNull, long loopFrequencyMs) {
    return loopUntilReply(sendRequestOrNull, loopFrequencyMs)
        .map(untypedPayload -> untypedPayload.read(AckResponse.class));
  }

  public <T> Single<T> loopUntilReply(
          CallbackWithArg<SorobanClient, Single<AbstractSorobanEndpoint<T>>> sendRequestOrNull, long loopFrequencyMs) {
    return rpcSession.loopUntil(
        // send request
        sendRequestOrNull,
        // wait for response
        (sorobanClient, endpoint) -> endpoint.waitNext(rpcSession, loopFrequencyMs));
  }


  public <T extends SorobanPayloadable> Single<T> loopUntilReplyTyped(
      CallbackWithArg<SorobanClient, Single<String>> sendRequestOrNull,
      long loopFrequencyMs,
      Class<T> typeReply) {
    return loopUntilReply(sendRequestOrNull, loopFrequencyMs)
        .map(untypedPayload -> untypedPayload.read(typeReply));
  }*/
}
