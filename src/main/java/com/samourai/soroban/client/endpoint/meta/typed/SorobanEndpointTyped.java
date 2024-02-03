package com.samourai.soroban.client.endpoint.meta.typed;

import com.samourai.soroban.client.AckResponse;
import com.samourai.soroban.client.SorobanClient;
import com.samourai.soroban.client.SorobanPayloadable;
import com.samourai.soroban.client.endpoint.SorobanApp;
import com.samourai.soroban.client.endpoint.meta.AbstractSorobanEndpointMeta;
import com.samourai.soroban.client.endpoint.meta.SorobanMetadata;
import com.samourai.soroban.client.endpoint.meta.SorobanMetadataImpl;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaEncryptWithSender;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaFilterType;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaType;
import com.samourai.soroban.client.endpoint.wrapper.SorobanWrapper;
import com.samourai.soroban.client.rpc.RpcMode;
import com.samourai.soroban.client.rpc.RpcSession;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.util.AsyncUtil;
import com.samourai.wallet.util.Pair;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import org.apache.commons.lang3.ArrayUtils;

/**
 * This endpoint sends & receives typed SorobanPayloadables objects, with full Soroban features
 * support. Payloads are wrapped with metadatas.
 */
public class SorobanEndpointTyped
    extends AbstractSorobanEndpointMeta<SorobanItemTyped, SorobanListTyped, SorobanPayloadable> {
  private static final AsyncUtil asyncUtil = AsyncUtil.getInstance();

  private Class[] replyTypesAllowedOrNull;

  public SorobanEndpointTyped(
      SorobanApp app,
      String path,
      RpcMode rpcMode,
      SorobanWrapper[] wrappers,
      Class[] typesAllowedOrNull,
      Class[] replyTypesAllowedOrNull) {
    super(app, path, rpcMode, computeWrappers(wrappers, typesAllowedOrNull));
    this.replyTypesAllowedOrNull = replyTypesAllowedOrNull;
  }

  public SorobanEndpointTyped(
      SorobanApp app,
      String path,
      RpcMode rpcMode,
      SorobanWrapper[] wrappers,
      Class[] typesAllowedOrNull) {
    this(app, path, rpcMode, wrappers, typesAllowedOrNull, null);
  }

  public SorobanEndpointTyped(
      SorobanApp app, String path, RpcMode rpcMode, SorobanWrapper[] wrappers) {
    this(app, path, rpcMode, wrappers, null, null);
  }

  private static SorobanWrapper[] computeWrappers(
      SorobanWrapper[] wrappers, Class[] typesAllowedOrNull) {
    SorobanWrapper wrapperType;
    if (typesAllowedOrNull != null) {
      wrapperType = new SorobanWrapperMetaFilterType(typesAllowedOrNull);
    } else {
      wrapperType = new SorobanWrapperMetaType();
    }
    return ArrayUtils.add(wrappers, wrapperType);
  }

  public Single<SorobanItemTyped> sendSingleAck(SorobanClient sorobanClient) throws Exception {
    return sendSingle(sorobanClient, new AckResponse());
  }

  public Completable sendAck(SorobanClient sorobanClient) throws Exception {
    return send(sorobanClient, new AckResponse());
  }

  @Override
  protected SorobanItemTyped newEntry(Pair<String, SorobanMetadata> entry, String rawEntry) {
    return new SorobanItemTyped(entry.getLeft(), entry.getRight(), rawEntry, this);
  }

  @Override
  protected Pair<String, SorobanMetadata> newEntry(SorobanPayloadable payload) throws Exception {
    return Pair.of(payload.toPayload(), new SorobanMetadataImpl());
  }

  @Override
  protected SorobanListTyped newList(List<SorobanItemTyped> items) {
    return new SorobanListTyped(items);
  }

  public <T> Single<T> waitNext(RpcSession rpcSession, Class<T> type) throws Exception {
    return super.waitNext(rpcSession).map(item -> item.read(type));
  }

  public Single<SorobanItemTyped> loopSendUntilReply(
      RpcSession rpcSession, SorobanPayloadable request) {
    return loopSendUntilReply(rpcSession, request, null);
  }

  public Single<SorobanItemTyped> loopSendUntilReply(
      RpcSession rpcSession, SorobanPayloadable request, Long waitReplyTimeoutMs) {
    Supplier<SorobanPayloadable> getRequest = () -> request;
    return loopSendUntilReply(rpcSession, getRequest, waitReplyTimeoutMs);
  }

  protected Single<SorobanItemTyped> loopSendUntilReply(
      RpcSession rpcSession, Supplier<SorobanPayloadable> getRequest) {
    return loopSendUntilReply(rpcSession, getRequest, null);
  }

  protected Single<SorobanItemTyped> loopSendUntilReply(
      RpcSession rpcSession, Supplier<SorobanPayloadable> getRequest, Long waitReplyTimeoutMs) {
    Callable<SorobanItemTyped> loop =
        () -> {
          // send request and wait reply with timeout
          SorobanPayloadable request = getRequest.get();
          return asyncUtil.blockingGet(sendAndWaitReply(rpcSession, request, waitReplyTimeoutMs));
        };
    return asyncUtil.runAndRetry(loop, waitReplyTimeoutMs);
  }

  public Single<SorobanItemTyped> sendAndWaitReply(
      RpcSession rpcSession, SorobanPayloadable request) {
    return sendAndWaitReply(rpcSession, request, null);
  }

  public Single<SorobanItemTyped> sendAndWaitReply(
      RpcSession rpcSession, SorobanPayloadable request, Long waitReplyTimeoutMs) {
    // waitingTime <= expirationMs
    long expirationMs = getRpcMode().getExpirationMs();
    long waitTimeoutMs;
    if (waitReplyTimeoutMs == null) {
      waitTimeoutMs = expirationMs;
    } else {
      waitTimeoutMs = Math.min(waitReplyTimeoutMs, expirationMs);
    }

    // send request
    try {
      return rpcSession
          .withSorobanClient(sorobanClient -> sendSingle(sorobanClient, request))
          // get reply with timeout
          .map(
              sorobanItem ->
                  asyncUtil.blockingGet(
                      getEndpointReply(sorobanItem).waitNext(rpcSession), waitTimeoutMs));
    } catch (Exception e) {
      return Single.error(e);
    }
  }

  public <R> Single<R> loopSendUntilReplyObject(
      RpcSession rpcSession,
      SorobanPayloadable request,
      long waitReplyTimeoutMs,
      Class<R> responseType) {
    Supplier<SorobanPayloadable> getRequest = () -> request;
    Single<SorobanItemTyped> result =
        loopSendUntilReply(rpcSession, getRequest, waitReplyTimeoutMs);
    return result.map(sorobanItem -> sorobanItem.read(responseType));
  }

  public Completable loopSendUntilReplyAck(
      RpcSession rpcSession, SorobanPayloadable request, long waitReplyTimeoutMs) {
    return Completable.fromSingle(
        loopSendUntilReplyObject(rpcSession, request, waitReplyTimeoutMs, AckResponse.class));
  }

  @Override
  public SorobanEndpointTyped getEndpointReply(SorobanItemTyped request) {
    String pathReply = getPathReply(request);
    // require sender for encryption
    PaymentCode sender = request.getMetaSender();
    return new SorobanEndpointTyped(
        getApp(),
        pathReply,
        RpcMode.SHORT,
        new SorobanWrapper[] {new SorobanWrapperMetaEncryptWithSender(sender)},
        replyTypesAllowedOrNull);
  }
}
