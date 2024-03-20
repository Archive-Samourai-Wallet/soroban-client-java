package com.samourai.soroban.client.endpoint.meta;

import com.samourai.soroban.client.SorobanClient;
import com.samourai.soroban.client.endpoint.AbstractSorobanEndpoint;
import com.samourai.soroban.client.endpoint.SorobanEndpoint;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMeta;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaNonce;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaSender;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaType;
import com.samourai.soroban.client.endpoint.wrapper.SorobanWrapper;
import com.samourai.soroban.client.endpoint.wrapper.SorobanWrapperString;
import com.samourai.soroban.client.exception.SorobanException;
import com.samourai.soroban.client.rpc.RpcMode;
import com.samourai.soroban.client.rpc.RpcSession;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.util.CallbackWithArg;
import com.samourai.wallet.util.Pair;
import com.samourai.wallet.util.Util;
import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSorobanEndpointMeta<I extends SorobanItem, S>
    extends AbstractSorobanEndpoint<I, S, SorobanMetadata, SorobanItemFilter<I>> {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String KEY_PAYLOAD = "payload";
  private static final String KEY_METADATA = "metadata";

  private List<SorobanWrapperMeta> wrappersMeta;
  private boolean decryptFromSender;

  private Map<String, Long> lastNonceByUniqueId; // TODO expiration
  private Set<String> lastUniqueIds; // TODO expiration
  private boolean useNonce;

  protected AbstractSorobanEndpointMeta(String dir, RpcMode rpcMode, SorobanWrapper[] wrappersAll) {
    this(dir, rpcMode, findWrappersString(wrappersAll), findWrappersMeta(wrappersAll));
  }

  protected AbstractSorobanEndpointMeta(
      String dir,
      RpcMode rpcMode,
      SorobanWrapperString[] wrappersString,
      List<SorobanWrapperMeta> wrappersMeta) {
    super(dir, rpcMode, wrappersString);
    this.wrappersMeta = wrappersMeta;
    this.lastUniqueIds = new LinkedHashSet<>();
    this.lastNonceByUniqueId = new LinkedHashMap<>();
    this.useNonce =
        wrappersMeta.stream().filter(w -> w instanceof SorobanWrapperMetaNonce).count() > 0;
  }

  protected static List<SorobanWrapperMeta> findWrappersMeta(SorobanWrapper[] wrappers) {
    return Arrays.stream(wrappers)
        .filter(wrapper -> SorobanWrapperMeta.class.isAssignableFrom(wrapper.getClass()))
        .map(w -> (SorobanWrapperMeta) w)
        .collect(Collectors.toList());
  }

  protected static SorobanWrapperString[] findWrappersString(SorobanWrapper[] wrappers) {
    SorobanWrapperString[] wrappersString =
        Arrays.stream(wrappers)
            .filter(wrapper -> SorobanWrapperString.class.isAssignableFrom(wrapper.getClass()))
            .toArray(n -> new SorobanWrapperString[n]);
    return wrappersString;
  }

  @Override
  protected Pair<String, SorobanMetadata> decryptOnReceive(
      Bip47Encrypter encrypter, Pair<String, SorobanMetadata> entry) throws Exception {
    if (decryptFromSender) {
      PaymentCode sender = SorobanWrapperMetaSender.getSender(entry.getRight());
      if (sender == null) {
        throw new SorobanException("missing request.metadata.sender for decryptFromSender");
      }
      return decryptFrom(encrypter, entry, sender);
    }
    return super.decryptOnReceive(encrypter, entry);
  }

  @Override
  public SorobanEndpoint getEndpointReply(I request, Bip47Encrypter encrypter) {
    AbstractSorobanEndpointMeta endpointReply =
        (AbstractSorobanEndpointMeta) super.getEndpointReply(request, encrypter);
    endpointReply.setEncryptReply(this, request, encrypter);
    return endpointReply;
  }

  @Override
  protected void setEncryptReply(
      AbstractSorobanEndpoint<I, S, SorobanMetadata, SorobanItemFilter<I>> endpointRequest,
      I request,
      Bip47Encrypter encrypter) {
    super.setEncryptReply(endpointRequest, request, encrypter);

    PaymentCode requestSender = request.getMetaSender();
    if (requestSender != null) {
      // request with metadata.sender (either not encrypted or encrypted)

      if (!encrypter.getPaymentCode().equals(requestSender)) {
        // received a request with metadata.sender => send reply encrypted for request.sender
        if (requestSender == null) {
          throw new RuntimeException(
              "Missing request.sender for setReplyEndpointEncryption() with decryptFromSender");
        }
        /*if (log.isDebugEnabled()) {
          log.debug("setEncryptReply: encrypt for request.sender");
        }*/
        this.setEncryptToWithSender(requestSender);
        /*if (endpointReq.getEncryptPartner() == null) {
          // request was not encrypted, so sender doesn't know our identity => add metadata.sender
          this.wrappersMeta.add(new SorobanWrapperMetaSender());
        }*/
      } else {
        // sent a request with my identity as metadata.sender
        // => read reply encrypted with reply.sender (which is unknown yet)
        this.setDecryptFromSender();
      }
    }
  }

  public AbstractSorobanEndpoint<I, S, SorobanMetadata, SorobanItemFilter<I>>
      setEncryptToWithSender(PaymentCode encryptTo) {
    this.setEncryptTo(encryptTo);
    this.wrappersMeta.add(new SorobanWrapperMetaSender());
    this.setDecryptFromSender();
    return this;
  }

  @Override
  protected String getRawEntry(SorobanItem entry) {
    return entry.getRawEntry();
  }

  @Override
  protected String entryToRaw(Pair<String, SorobanMetadata> entry) throws Exception {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(KEY_PAYLOAD, entry.getLeft());
    jsonObject.put(KEY_METADATA, entry.getRight().toJsonObject());
    return jsonObject.toString();
  }

  @Override
  protected Pair<String, SorobanMetadata> rawToEntry(String rawEntry) throws Exception {
    JSONObject jsonObject = new JSONObject(rawEntry);
    String payload = jsonObject.getString(KEY_PAYLOAD);
    JSONObject jsonObjectMeta = jsonObject.getJSONObject(KEY_METADATA);
    SorobanMetadata metadata = new SorobanMetadataImpl(jsonObjectMeta);
    return Pair.of(payload, metadata);
  }

  @Override
  protected Pair<String, SorobanMetadata> applyWrappersOnSend(
      Bip47Encrypter encrypter, Pair<String, SorobanMetadata> entry, Object initialPayload)
      throws Exception {
    // apply meta wrappers
    for (SorobanWrapperMeta wrapperMeta : wrappersMeta) {
      entry = wrapperMeta.onSend(encrypter, entry, initialPayload);
    }

    // apply string wrappers
    return super.applyWrappersOnSend(encrypter, entry, initialPayload);
  }

  @Override
  protected Pair<String, SorobanMetadata> applyWrappersOnReceive(
      Bip47Encrypter encrypter, Pair<String, SorobanMetadata> entry) throws Exception {
    // apply string wrappers
    entry = super.applyWrappersOnReceive(encrypter, entry);

    // apply meta wrappers
    try {
      for (SorobanWrapperMeta wrapperMeta : wrappersMeta) {
        entry = wrapperMeta.onReceive(encrypter, entry);
      }
    } catch (Exception e) {
      log.warn(" <- sorobanDir=" + getDir() + ": " + entry + ": INVALID: " + e.getMessage());
      throw e;
    }
    return entry;
  }

  public String computeUniqueId(I entry) {
    List<String> uniqueParts = new LinkedList<>();
    uniqueParts.add(entry.getPayload()); // decrypted payload
    PaymentCode sender = entry.getMetaSender();
    if (sender != null) {
      uniqueParts.add(sender.toString());
    }
    String type = SorobanWrapperMetaType.getType(entry.getMetadata());
    if (type != null) {
      uniqueParts.add(type);
    }
    String uniqueId = StringUtils.join(uniqueParts, "|");
    return Util.sha256Hex(uniqueId);
  }

  @Override
  protected SorobanItemFilter<I> newFilterBuilder() {
    return new SorobanItemFilter<>();
  }

  @Override
  protected SorobanItemFilter<I> createFilter(Consumer<SorobanItemFilter<I>> f) {
    SorobanItemFilter<I> filter = super.createFilter(f);
    return filter;
  }

  @Override
  protected Stream<I> applyFilterNoReplay(Stream<I> stream) {
    if (isNoReplay()) {
      if (useNonce) {
        // filter per uniqueId with greater nonce
        return stream.filter(
            sorobanItem -> {
              String uniqueId = sorobanItem.getUniqueId();
              Long nonce = sorobanItem.getMetaNonce();
              Long lastNonce = lastNonceByUniqueId.get(uniqueId);
              return lastNonce == null || nonce == null || sorobanItem.getMetaNonce() > lastNonce;
            });
      } else {
        // filter per uniqueId
        return stream.filter(i -> !lastUniqueIds.contains(i.getUniqueId()));
      }
    }
    return stream;
  }

  @Override
  protected void onReadNoReplay(I item) {
    if (isNoReplay()) {
      if (useNonce) {
        // save last nonce per uniqueId
        Long nonce = item.getMetaNonce();
        if (nonce != null) {
          Long lastNonce = lastNonceByUniqueId.get(item.getUniqueId());
          if (lastNonce != null && lastNonce >= nonce) {
            log.error(
                "NONCE REPLAY! "
                    + item
                    + " lastNonce="
                    + lastNonce
                    + " lastNonceByUniqueId="
                    + lastNonceByUniqueId); // TODO
          }
          lastNonceByUniqueId.put(item.getUniqueId(), nonce);
        }
      } else {
        // save last uniqueId
        lastUniqueIds.add(item.getUniqueId());
      }
    }
  }

  // WAIT REPLY

  public I loopWaitReply(RpcSession rpcSession, I request) throws Exception {
    return loopWaitReply(rpcSession, request, null, null);
  }

  public I loopWaitReply(RpcSession rpcSession, I request, Integer replyTimeoutMs)
      throws Exception {
    return loopWaitReply(rpcSession, request, replyTimeoutMs, null);
  }

  public I loopWaitReply(RpcSession rpcSession, I request, Consumer<SorobanItemFilter<I>> filter)
      throws Exception {
    return loopWaitReply(rpcSession, request, null, filter);
  }

  // wait during replyTimeoutMs (default=endpoint.expirationMs) at endpoint.polling frequency
  public I loopWaitReply(
      RpcSession rpcSession, I request, Integer timeoutMs, Consumer<SorobanItemFilter<I>> filter)
      throws Exception {
    // if no timeout set, wait until initial request expired
    if (timeoutMs == null) {
      timeoutMs = getExpirationMs();
    }

    // get reply with timeout
    Bip47Encrypter encrypter = rpcSession.getRpcWallet().getBip47Encrypter();
    SorobanEndpoint endpointReply = getEndpointReply(request, encrypter);
    try {
      if (log.isTraceEnabled()) {
        log.trace(
            "waitReply("
                + timeoutMs
                + " at "
                + getPollingFrequencyMs()
                + " frequency) sorobanDir="
                + endpointReply.getDir());
      }
      return (I) endpointReply.loopWaitAny(rpcSession, timeoutMs, filter);
    } catch (TimeoutException e) {
      if (log.isTraceEnabled()) {
        log.trace(
            "waitReply() sorobanDir="
                + endpointReply.getDir()
                + ": Timeout after "
                + timeoutMs
                + "ms");
      }
      throw e;
    }
  }

  // SEND AND WAIT REPLY

  // send, then wait during replyTimeoutMs (default=endpoint.expirationMs) at endpoint.polling
  // frequency
  public I sendAndWaitReply(RpcSession rpcSession, S request) throws Exception {
    return sendAndWaitReply(rpcSession, request, null, null);
  }

  public I sendAndWaitReply(RpcSession rpcSession, S request, Integer replyTimeoutMs)
      throws Exception {
    return sendAndWaitReply(rpcSession, request, replyTimeoutMs, null);
  }

  // send, then wait during replyTimeoutMs (default=endpoint.expirationMs) at endpoint.polling
  // frequency
  public I sendAndWaitReply(RpcSession rpcSession, S request, Consumer<SorobanItemFilter<I>> filter)
      throws Exception {
    return sendAndWaitReply(rpcSession, request, null, filter);
  }

  public I sendAndWaitReply(
      RpcSession rpcSession,
      S request,
      Integer replyTimeoutMs,
      Consumer<SorobanItemFilter<I>> filter)
      throws Exception {
    // send request
    I req =
        asyncUtil.blockingGet(
            rpcSession.withSorobanClientSingle(
                sorobanClient -> sendSingle(sorobanClient, request)));
    // wait reply
    return loopWaitReply(rpcSession, req, replyTimeoutMs, filter);
  }

  // LOOP SEND UNTIL REPLY

  public <T> T loopSendUntil(
      RpcSession rpcSession, S request, int timeoutMs, CallbackWithArg<I, T> loopUntil)
      throws Exception {
    return loopSendUntil(rpcSession, () -> request, timeoutMs, loopUntil);
  }

  // loop {sending request then getReply()), at endpoint.resendFrequencyWhenNoReplyMs
  public <T> T loopSendUntil(
      RpcSession rpcSession, Supplier<S> getRequest, int timeoutMs, CallbackWithArg<I, T> getReply)
      throws Exception {
    /*if (log.isDebugEnabled()) {
      log.debug("NEW LOOP_SEND_UNTIL(" + timeoutMs + ")");
    }*/
    long timeStart = System.currentTimeMillis();
    CallbackWithArg<SorobanClient, Optional<T>> loop =
        sorobanClient -> {
          // TODO enforce timeout, seems that .timeout() is not always working
          int elapsedMs = (int) (System.currentTimeMillis() - timeStart);
          /*if (elapsedMs > timeoutMs) {
            if (log.isDebugEnabled()) {
              log.debug("loopSendUntil() => exit timeout("+elapsedMs+")");
            }
            throw new TimeoutException("loopSendUntil() => exit timeout("+elapsedMs+")");
          }*/

          // send request
          S request = getRequest.get();
          /*if (log.isDebugEnabled()) {
            log.debug(
                "CYCLE_LOOP_SEND_UNTIL ("
                    + (elapsedMs / 1000)
                    + "/"
                    + (timeoutMs / 1000)
                    + ") => SEND_REQUEST "
                    + request);
          }*/
          I requestItem = asyncUtil.blockingGet(sendSingle(sorobanClient, request));
          // wait reply
          /*if (log.isDebugEnabled()) {
            log.debug("CYCLE_LOOP_SEND_UNTIL => WAIT_REPLY");
          }*/
          T reply = getReply.apply(requestItem);
          /*if (log.isDebugEnabled()) {
            log.debug("CYCLE_LOOP_SEND_UNTIL => SUCCESS " + reply);
          }*/
          return Optional.of(reply);
        };

    // with global timeout
    return rpcSession.loopUntilSuccess(loop, getResendFrequencyWhenNoReplyMs(), timeoutMs);
  }

  // LOOP SEND AND WAIT REPLY

  // loop {send, then wait reply at endpoint.pollingFrequency} at
  // endpoint.resendFrequencyWhenNoReply

  public I loopSendAndWaitReply(RpcSession rpcSession, S request) throws Exception {
    return loopSendAndWaitReply(rpcSession, request, getDefaultLoopTimeoutMs(), null);
  }

  public I loopSendAndWaitReply(
      RpcSession rpcSession, S request, Consumer<SorobanItemFilter<I>> filter) throws Exception {
    return loopSendAndWaitReply(rpcSession, request, getDefaultLoopTimeoutMs(), filter);
  }

  public I loopSendAndWaitReply(RpcSession rpcSession, S request, int timeoutMs) throws Exception {
    return loopSendAndWaitReply(rpcSession, request, timeoutMs, null);
  }

  public I loopSendAndWaitReply(
      RpcSession rpcSession, S request, int timeoutMs, Consumer<SorobanItemFilter<I>> filter)
      throws Exception {
    return loopSendAndWaitReply(
        rpcSession, request, timeoutMs, filter, getResendFrequencyWhenNoReplyMs());
  }

  private I loopSendAndWaitReply(
      RpcSession rpcSession,
      S request,
      int timeoutMs,
      Consumer<SorobanItemFilter<I>> filter,
      Integer sendFrequencyMs)
      throws Exception {
    int sendFrequencyMsCapped = Math.min(timeoutMs, sendFrequencyMs);
    return loopSendUntil(
        rpcSession,
        request,
        timeoutMs,
        req ->
            // wait reply
            loopWaitReply(rpcSession, req, sendFrequencyMsCapped, filter));
  }

  public AbstractSorobanEndpointMeta<I, S> setDecryptFromSender() {
    this.decryptFromSender = true;
    wrappersMeta.add(new SorobanWrapperMetaSender()); // add metadata.sender
    return this;
  }

  public boolean isDecryptFromSender() {
    return decryptFromSender;
  }

  protected List<SorobanWrapperMeta> getWrappersMeta() {
    return wrappersMeta;
  }

  public String getDirReply(I entry) {
    return getDir() + "/REPLY/" + computeUniqueId(entry);
  }
}
