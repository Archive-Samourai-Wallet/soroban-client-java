package com.samourai.soroban.client.rpc;

import com.samourai.soroban.client.*;
import com.samourai.wallet.bip47.rpc.Bip47Partner;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcSessionPartnerApi extends RpcSessionApi {
  private static final Logger log = LoggerFactory.getLogger(RpcSessionPartnerApi.class);

  protected final Bip47Partner bip47Partner;
  protected final Function<String, String> directoryReply;

  public RpcSessionPartnerApi(
      RpcSession rpcSession, Bip47Partner bip47Partner, Function<String, String> directoryReply) {
    super(rpcSession);
    this.bip47Partner = bip47Partner;
    this.directoryReply = directoryReply;
  }

  protected String getDirectoryReply(String requestId) {
    return directoryReply.apply(requestId);
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

  public Single<AbstractSorobanPayloadable> loopUntilReply(
      CallbackWithArg<SorobanClient, Single<String>> sendRequestOrNull, long loopFrequencyMs) {
    return rpcSession.loopUntil(
        // send request
        sendRequestOrNull,
        // wait for response
        (sorobanClient, requestId) -> waitReply(loopFrequencyMs, requestId, sorobanClient));
  }

  public <T extends SorobanPayloadable> Single<T> loopUntilReplyTyped(
      CallbackWithArg<SorobanClient, Single<String>> sendRequestOrNull,
      long loopFrequencyMs,
      Class<T> typeReply) {
    return loopUntilReply(sendRequestOrNull, loopFrequencyMs)
        .map(untypedPayload -> untypedPayload.read(typeReply));
  }

  protected Single<AbstractSorobanPayloadable> waitReply(
      long timeoutMs, String requestId, SorobanClient sorobanClient) throws Exception {
    // TODO iterate until good type + wrap dialog messages with message type
    String directory = getDirectoryReply(requestId);
    return rpcSession
        .directoryValueWaitAndRemove(directory, timeoutMs)
        .map(
            payload -> {
              // decrypt
              AbstractSorobanPayloadable decryptedPayload =
                  sorobanClient.readEncrypted(payload, bip47Partner.getPaymentCodePartner());
              if (log.isDebugEnabled()) {
                log.debug(
                    " <- "
                        + RpcClient.shortDirectory(directory)
                        + ": "
                        + decryptedPayload.getPayload());
              }
              return decryptedPayload;
            });
  }*/
}
