package com.samourai.soroban.client.rpc;

import com.samourai.soroban.client.AckResponse;
import com.samourai.soroban.client.SorobanClient;
import com.samourai.soroban.client.SorobanPayload;
import com.samourai.soroban.client.UntypedPayload;
import com.samourai.wallet.bip47.rpc.Bip47Partner;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.util.CallbackWithArg;
import com.samourai.wallet.util.Util;
import io.reactivex.Single;
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

  public Single<String> sendEncryptedWithSender(SorobanPayload sorobanPayload, String directory)
      throws Exception {
    return rpcSession.withSorobanClient(
        sorobanClient -> sendEncryptedWithSender(sorobanPayload, directory, sorobanClient));
  }

  public Single<String> sendEncryptedWithSender(
      SorobanPayload sorobanPayload, String directory, SorobanClient sorobanClient)
      throws Exception {
    PaymentCode paymentCodePartner = bip47Partner.getPaymentCodePartner();
    String sorobanPayloadStr = sorobanPayload.toPayload();
    if (log.isDebugEnabled()) {
      log.debug(
          " -> "
              + " ["
              + sorobanPayload.getClass().getSimpleName()
              + "] "
              + RpcClient.shortDirectory(directory)
              + " ("
              + Util.maskString(paymentCodePartner.toString())
              + ")"
              + sorobanPayloadStr);
    }
    String encryptedPayload =
        sorobanClient.encryptWithSender(sorobanPayloadStr, paymentCodePartner);
    return sorobanClient
        .getRpcClient()
        .directoryAdd(directory, encryptedPayload, RpcMode.SHORT)
        .toSingle(() -> getRequestId(sorobanPayloadStr));
  }

  public Single<String> sendReplyEncrypted(SorobanPayload sorobanPayload, String requestId)
      throws Exception {
    return rpcSession.withSorobanClient(
        sorobanClient -> sendReplyEncrypted(sorobanPayload, requestId, sorobanClient));
  }

  public Single<String> sendReplyEncrypted(
      SorobanPayload sorobanPayload, String requestId, SorobanClient sorobanClient)
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
  }

  //

  public Single<UntypedPayload> loopUntilReply(String requestId, long loopFrequencyMs) {
    return loopUntilReply(sorobanClient -> Single.just(requestId), loopFrequencyMs);
  }

  public <T extends SorobanPayload> Single<T> loopUntilReplyTyped(
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

  public Single<UntypedPayload> loopUntilReply(
      CallbackWithArg<SorobanClient, Single<String>> sendRequestOrNull, long loopFrequencyMs) {
    return rpcSession.loopUntil(
        // send request
        sendRequestOrNull,
        // wait for response
        (sorobanClient, requestId) -> waitReply(loopFrequencyMs, requestId, sorobanClient));
  }

  public <T extends SorobanPayload> Single<T> loopUntilReplyTyped(
      CallbackWithArg<SorobanClient, Single<String>> sendRequestOrNull,
      long loopFrequencyMs,
      Class<T> typeReply) {
    return loopUntilReply(sendRequestOrNull, loopFrequencyMs)
        .map(untypedPayload -> untypedPayload.read(typeReply));
  }

  protected Single<UntypedPayload> waitReply(
      long timeoutMs, String requestId, SorobanClient sorobanClient) throws Exception {
    // TODO iterate until good type + wrap dialog messages with message type
    String directory = getDirectoryReply(requestId);
    return rpcSession
        .directoryValueWaitAndRemove(directory, timeoutMs)
        .map(
            payload -> {
              // decrypt
              UntypedPayload untypedPayload =
                  sorobanClient.readEncrypted(payload, bip47Partner.getPaymentCodePartner());
              if (log.isDebugEnabled()) {
                log.debug(
                    " <- "
                        + RpcClient.shortDirectory(directory)
                        + ": "
                        + untypedPayload.getPayload());
              }
              return untypedPayload;
            });
  }
}
