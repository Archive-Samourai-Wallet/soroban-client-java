package com.samourai.soroban.client.endpoint.meta.typed;

import com.samourai.soroban.client.SorobanClient;
import com.samourai.soroban.client.endpoint.meta.AbstractSorobanEndpointMeta;
import com.samourai.soroban.client.endpoint.meta.SorobanItemFilter;
import com.samourai.soroban.client.endpoint.meta.SorobanMetadata;
import com.samourai.soroban.client.endpoint.meta.SorobanMetadataImpl;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMeta;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaFilterType;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaType;
import com.samourai.soroban.client.endpoint.wrapper.SorobanWrapper;
import com.samourai.soroban.client.endpoint.wrapper.SorobanWrapperString;
import com.samourai.soroban.client.rpc.RpcMode;
import com.samourai.soroban.client.rpc.RpcSession;
import com.samourai.soroban.protocol.payload.AckResponse;
import com.samourai.soroban.protocol.payload.SorobanErrorMessage;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.sorobanClient.SorobanPayloadable;
import com.samourai.wallet.util.CallbackWithArg;
import com.samourai.wallet.util.Pair;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.lang.invoke.MethodHandles;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This endpoint sends & receives typed SorobanPayloadables objects, with full Soroban features
 * support. Payloads are wrapped with metadatas.
 */
public class SorobanEndpointTyped
    extends AbstractSorobanEndpointMeta<SorobanItemTyped, SorobanPayloadable> {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private Class[] replyTypesAllowedOrNull;

  public SorobanEndpointTyped(
      String dir,
      RpcMode rpcMode,
      SorobanWrapper[] wrappersAll,
      Class[] typesAllowedOrNull,
      Class[] replyTypesAllowedOrNull) {
    this(
        dir,
        rpcMode,
        findWrappersString(wrappersAll),
        findWrappersMeta(wrappersAll, typesAllowedOrNull),
        replyTypesAllowedOrNull);
  }

  public SorobanEndpointTyped(
      String dir, RpcMode rpcMode, SorobanWrapper[] wrappersAll, Class[] typesAllowedOrNull) {
    this(dir, rpcMode, wrappersAll, typesAllowedOrNull, null);
  }

  public SorobanEndpointTyped(String dir, RpcMode rpcMode, SorobanWrapper[] wrappersAll) {
    this(dir, rpcMode, wrappersAll, null);
  }

  protected SorobanEndpointTyped(
      String dir,
      RpcMode rpcMode,
      SorobanWrapperString[] wrappers,
      List<SorobanWrapperMeta> wrapperMetas,
      Class[] replyTypesAllowedOrNull) {
    super(dir, rpcMode, wrappers, wrapperMetas);
    this.replyTypesAllowedOrNull = replyTypesAllowedOrNull;
  }

  public SorobanEndpointTyped(SorobanEndpointTyped copy) {
    this(
        copy.getDir(),
        copy.getRpcMode(),
        copy.getWrappers().toArray(new SorobanWrapperString[] {}),
        copy.getWrappersMeta(),
        copy.replyTypesAllowedOrNull);
  }

  private static List<SorobanWrapperMeta> findWrappersMeta(
      SorobanWrapper[] wrappers, Class[] typesAllowedOrNull) {
    SorobanWrapperMeta wrapperType;
    if (typesAllowedOrNull != null) {
      wrapperType = new SorobanWrapperMetaFilterType(typesAllowedOrNull);
    } else {
      wrapperType = new SorobanWrapperMetaType();
    }
    List<SorobanWrapperMeta> wrapperMetas = AbstractSorobanEndpointMeta.findWrappersMeta(wrappers);
    wrapperMetas.add(wrapperType);
    return wrapperMetas;
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
  public SorobanEndpointTyped newEndpointReply(SorobanItemTyped request, Bip47Encrypter encrypter) {
    String dirReply = getDirReply(request);
    Class[] replyTypes = computeReplyTypes(replyTypesAllowedOrNull);
    SorobanEndpointTyped endpoint =
        new SorobanEndpointTyped(dirReply, getReplyRpcMode(), new SorobanWrapper[] {}, replyTypes);
    endpoint.setEncryptReply(this, request, encrypter);
    return endpoint;
  }

  private <R> Single<List<R>> doGetListObjects(
      SorobanClient sorobanClient,
      Class<?> type,
      Consumer<SorobanItemFilter<SorobanItemTyped>> filterBuilder,
      CallbackWithArg<SorobanItemTyped, R> mapResult) {
    return getList(
            sorobanClient,
            filter -> {
              // filter by type
              filter.filterByType(type);
              // apply custom filter
              if (filterBuilder != null) {
                filterBuilder.accept(filter);
              }
            })
        .map(
            list ->
                // map to objects
                list.stream()
                    .map(
                        i -> {
                          try {
                            return mapResult.apply(i);
                          } catch (Exception e) {
                            log.error(
                                "getListObjects(): item ignored due to error on " + toString(), e);
                            return null;
                          }
                        })
                    .filter(i -> i != null)
                    .collect(Collectors.toList()));
  }

  public <T> Single<List<T>> getListObjects(SorobanClient sorobanClient, Class<T> type) {
    return getListObjects(sorobanClient, type, null);
  }

  public <T> Single<List<T>> getListObjects(
      SorobanClient sorobanClient,
      Class<T> type,
      Consumer<SorobanItemFilter<SorobanItemTyped>> filterBuilder) {
    return doGetListObjects(sorobanClient, type, filterBuilder, i -> i.read(type));
  }

  public <T> Single<List<Pair<T, SorobanMetadata>>> getListObjectsWithMetadata(
      SorobanClient sorobanClient, Class<T> type) {
    return getListObjectsWithMetadata(sorobanClient, type, null);
  }

  public <T> Single<List<Pair<T, SorobanMetadata>>> getListObjectsWithMetadata(
      SorobanClient sorobanClient,
      Class<T> type,
      Consumer<SorobanItemFilter<SorobanItemTyped>> filterBuilder) {
    return doGetListObjects(
        sorobanClient, type, filterBuilder, i -> Pair.of(i.read(type), i.getMetadata()));
  }

  // FIND ANY
  public <T> Single<Optional<T>> findAnyObject(SorobanClient sorobanClient, Class<T> type) {
    return findAnyObject(sorobanClient, type, null);
  }

  public <T> Single<Optional<T>> findAnyObject(
      SorobanClient sorobanClient,
      Class<T> type,
      Consumer<SorobanItemFilter<SorobanItemTyped>> filterBuilderOrNull) {
    return findAny(sorobanClient, filterBuilderOrNull)
        .map(
            sorobanItemTyped -> {
              if (!sorobanItemTyped.isPresent()) {
                return Optional.empty();
              }
              return sorobanItemTyped.get().readOn(type);
            });
  }

  public <T> Single<Optional<Pair<T, SorobanMetadata>>> findAnyObjectWithMetadata(
      SorobanClient sorobanClient, Class<T> type) {
    return findAnyObjectWithMetadata(sorobanClient, type, null);
  }

  public <T> Single<Optional<Pair<T, SorobanMetadata>>> findAnyObjectWithMetadata(
      SorobanClient sorobanClient,
      Class<T> type,
      Consumer<SorobanItemFilter<SorobanItemTyped>> filterBuilderOrNull) {
    return findAny(sorobanClient, filterBuilderOrNull)
        .map(
            sorobanItemTyped -> {
              if (!sorobanItemTyped.isPresent()) {
                return Optional.empty();
              }
              return sorobanItemTyped
                  .get()
                  .readOn(type)
                  .map(o -> Pair.of(o, sorobanItemTyped.get().getMetadata()));
            });
  }

  // WAIT FIRST

  public <T> T waitAnyObject(RpcSession rpcSession, Class<T> type, long timeoutMs)
      throws Exception {
    return waitAnyObject(rpcSession, type, timeoutMs, null);
  }

  public <T> T waitAnyObject(
      RpcSession rpcSession,
      Class<T> type,
      long timeoutMs,
      Consumer<SorobanItemFilter<SorobanItemTyped>> filterBuilderOrNull)
      throws Exception {
    return (T) waitAnyObjects(rpcSession, new Class[] {type}, timeoutMs, filterBuilderOrNull);
  }

  public <T> T waitAnyObjects(RpcSession rpcSession, Class<T>[] types, long timeoutMs)
      throws Exception {
    return waitAnyObjects(rpcSession, types, timeoutMs, null);
  }

  // loop until value found for such types, at endpoint.polling frequency
  public <T> T waitAnyObjects(
      RpcSession rpcSession,
      Class<T>[] types,
      long timeoutMs,
      Consumer<SorobanItemFilter<SorobanItemTyped>> filterBuilderOrNull)
      throws Exception {
    Consumer<SorobanItemFilter<SorobanItemTyped>> filter =
        f -> {
          // extend filter
          if (filterBuilderOrNull != null) {
            filterBuilderOrNull.accept(f);
          }
          // filter by type
          f.filterByType(types);
        };
    SorobanItemTyped item = loopWaitAny(rpcSession, timeoutMs, filter);
    return (T) item.read();
  }

  // WAIT REPLY

  public <T> T waitReplyObject(RpcSession rpcSession, SorobanItemTyped request, Class<T> type)
      throws Exception {
    return waitReplyObject(rpcSession, request, type, null, null);
  }

  public <T> T waitReplyObject(
      RpcSession rpcSession, SorobanItemTyped request, Class<T> type, Integer replyTimeoutMs)
      throws Exception {
    return waitReplyObject(rpcSession, request, type, replyTimeoutMs, null);
  }

  /*public <T> T waitReplyObject(
      RpcSession rpcSession,
      SorobanItemTyped request,
      Class<T> type,
      Consumer<SorobanItemFilter<SorobanItemTyped>> filterBuilderOrNull)
      throws Exception {
    return waitReplyObject(rpcSession, request, type, null, filterBuilderOrNull);
  }*/

  // overridable
  protected Class[] computeReplyTypes(Class... classesAllowed) {
    if (classesAllowed == null) {
      // no type restriction
      return null;
    }

    List<Class> listAllowed = new LinkedList<>();
    for (Class c : classesAllowed) {
      listAllowed.add(c);
    }
    // always accept SorobanErrorMessage to allow error responses
    listAllowed.add(SorobanErrorMessage.class);
    return listAllowed.toArray(new Class[] {});
  }

  // wait during replyTimeoutMs (default=endpoint.expirationMs) at endpoint.polling frequency
  private <T> T waitReplyObject(
      RpcSession rpcSession,
      SorobanItemTyped request,
      Class<T> type,
      Integer replyTimeoutMs,
      Consumer<SorobanItemFilter<SorobanItemTyped>> filterBuilderOrNull)
      throws Exception {
    Consumer<SorobanItemFilter<SorobanItemTyped>> filter =
        f -> {
          // extend filter
          if (filterBuilderOrNull != null) {
            filterBuilderOrNull.accept(f);
          }
          // filter by type
          Class[] replyTypes = computeReplyTypes(type);
          f.filterByType(replyTypes);
        };
    SorobanItemTyped item = super.loopWaitReply(rpcSession, request, replyTimeoutMs, filter);
    return item.read(type);
  }

  public AckResponse waitReplyAck(RpcSession rpcSession, SorobanItemTyped request)
      throws Exception {
    return waitReplyAck(rpcSession, request, null);
  }

  public AckResponse waitReplyAck(
      RpcSession rpcSession, SorobanItemTyped request, Integer replyTimeoutMs) throws Exception {
    return waitReplyObject(rpcSession, request, AckResponse.class, replyTimeoutMs, null);
  }

  // SEND AND WAIT REPLY

  public <T> T sendAndWaitReplyObject(
      RpcSession rpcSession, SorobanPayloadable request, Class<T> type) throws Exception {
    return sendAndWaitReplyObject(rpcSession, request, type, null, null);
  }

  public <T> T sendAndWaitReplyObject(
      RpcSession rpcSession, SorobanPayloadable request, Class<T> type, Integer replyTimeoutMs)
      throws Exception {
    return sendAndWaitReplyObject(rpcSession, request, type, replyTimeoutMs, null);
  }

  /*public <T> Single<T> sendAndWaitReplyObject(RpcSession rpcSession, SorobanPayloadable request, Class<T> type, SorobanItemFilter<SorobanItemTyped> filter) {
    return sendAndWaitReplyObject(rpcSession, request, type, null, filter);
  }*/

  // send request, then wait during replyTimeoutMs (default=endpoint.expirationMs) at
  // endpoint.polling frequency
  private <T> T sendAndWaitReplyObject(
      RpcSession rpcSession,
      SorobanPayloadable request,
      Class<T> type,
      Integer replyTimeoutMs,
      Consumer<SorobanItemFilter<SorobanItemTyped>> filter)
      throws Exception {
    // send request
    SorobanItemTyped req =
        asyncUtil.blockingGet(
            rpcSession.withSorobanClientSingle(
                sorobanClient -> sendSingle(sorobanClient, request)));
    // wait reply
    return waitReplyObject(rpcSession, req, type, replyTimeoutMs, filter);
  }

  public void sendAndWaitReplyAck(RpcSession rpcSession, SorobanPayloadable request)
      throws Exception {
    sendAndWaitReplyAck(rpcSession, request, null, null);
  }

  public void sendAndWaitReplyAck(
      RpcSession rpcSession, SorobanPayloadable request, Integer replyTimeoutMs) throws Exception {
    sendAndWaitReplyAck(rpcSession, request, replyTimeoutMs, null);
  }

  private void sendAndWaitReplyAck(
      RpcSession rpcSession,
      SorobanPayloadable request,
      Integer replyTimeoutMs,
      Consumer<SorobanItemFilter<SorobanItemTyped>> filter)
      throws Exception {
    sendAndWaitReplyObject(rpcSession, request, AckResponse.class, replyTimeoutMs, filter);
  }

  // LOOP SEND AND WAIT REPLY

  // loop {send, then wait reply at endpoint.pollingFrequency} at
  // endpoint.resendFrequencyWhenNoReply

  public <T> T loopSendAndWaitReplyObject(
      RpcSession rpcSession, SorobanPayloadable request, Class<T> replyType) throws Exception {
    return loopSendAndWaitReplyObject(rpcSession, request, replyType, getDefaultLoopTimeoutMs());
  }

  public <T> T loopSendAndWaitReplyObject(
      RpcSession rpcSession,
      SorobanPayloadable request,
      Class<T> replyType,
      Consumer<SorobanItemFilter<SorobanItemTyped>> filter)
      throws Exception {
    return loopSendAndWaitReplyObject(
        rpcSession, request, replyType, getDefaultLoopTimeoutMs(), filter);
  }

  public <T> T loopSendAndWaitReplyObject(
      RpcSession rpcSession, SorobanPayloadable request, Class<T> replyType, int timeoutMs)
      throws Exception {
    return loopSendAndWaitReplyObject(
        rpcSession, request, replyType, timeoutMs, null, getResendFrequencyWhenNoReplyMs());
  }

  public <T> T loopSendAndWaitReplyObject(
      RpcSession rpcSession,
      SorobanPayloadable request,
      Class<T> replyType,
      int timeoutMs,
      Consumer<SorobanItemFilter<SorobanItemTyped>> filter)
      throws Exception {
    return loopSendAndWaitReplyObject(
        rpcSession, request, replyType, timeoutMs, filter, getResendFrequencyWhenNoReplyMs());
  }

  private <T> T loopSendAndWaitReplyObject(
      RpcSession rpcSession,
      SorobanPayloadable request,
      Class<T> replyType,
      int timeoutMs,
      Consumer<SorobanItemFilter<SorobanItemTyped>> filter,
      Integer sendFrequencyMs)
      throws Exception {
    // important: cap with timeoutMs to avoid running multiple subloops
    int sendFrequencyMsCapped = Math.min(timeoutMs, sendFrequencyMs);
    return loopSendUntil(
        rpcSession,
        request,
        timeoutMs,
        req ->
            // wait reply
            waitReplyObject(rpcSession, req, replyType, sendFrequencyMsCapped, filter));
  }

  public void loopSendAndWaitReplyAck(
      RpcSession rpcSession, SorobanPayloadable request, int timeoutMs) throws Exception {
    loopSendAndWaitReplyAck(rpcSession, request, timeoutMs, getResendFrequencyWhenNoReplyMs());
  }

  private void loopSendAndWaitReplyAck(
      RpcSession rpcSession, SorobanPayloadable request, int timeoutMs, Integer sendFrequencyMs)
      throws Exception {
    loopSendUntil(
        rpcSession,
        request,
        timeoutMs,
        req ->
            // wait reply
            waitReplyAck(rpcSession, req, sendFrequencyMs));
  }

  @Override
  public SorobanEndpointTyped setDecryptFromSender() {
    return (SorobanEndpointTyped) super.setDecryptFromSender();
  }

  @Override
  public SorobanEndpointTyped setDecryptFrom(PaymentCode decryptFrom) {
    return (SorobanEndpointTyped) super.setDecryptFrom(decryptFrom);
  }

  @Override
  public SorobanEndpointTyped setEncryptTo(PaymentCode encryptPartner) {
    return (SorobanEndpointTyped) super.setEncryptTo(encryptPartner);
  }

  @Override
  public SorobanEndpointTyped setEncryptToWithSender(PaymentCode encryptTo) {
    return (SorobanEndpointTyped) super.setEncryptToWithSender(encryptTo);
  }
}
