package com.samourai.soroban.client.endpoint;

import com.samourai.soroban.client.SorobanClient;
import com.samourai.soroban.client.endpoint.meta.SorobanFilter;
import com.samourai.soroban.client.endpoint.wrapper.SorobanWrapperString;
import com.samourai.soroban.client.rpc.RpcMode;
import com.samourai.soroban.client.rpc.RpcSession;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.util.AsyncUtil;
import com.samourai.wallet.util.Pair;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSorobanEndpoint<I, S, M, F extends SorobanFilter<I>>
    implements SorobanEndpoint<I, S, F> {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  protected static final AsyncUtil asyncUtil = AsyncUtil.getInstance();

  private String dir;
  private RpcMode rpcMode;
  private RpcMode replyRpcMode;
  private List<SorobanWrapperString> wrappers;
  private PaymentCode encryptTo; // may be null
  private PaymentCode decryptFrom; // may be null
  private int pollingFrequencyMs;
  private int resendFrequencyWhenNoReplyMs;

  private boolean
      noReplay; // prevent reading the same message twice: same requestId(/nonce when available)
  private boolean autoRemove;

  public AbstractSorobanEndpoint(String dir, RpcMode rpcMode, SorobanWrapperString[] wrappers) {
    this.dir = dir;
    this.rpcMode = rpcMode;
    this.replyRpcMode = RpcMode.FAST;
    this.wrappers = Arrays.asList(wrappers);
    this.encryptTo = null;
    this.decryptFrom = null;
    this.pollingFrequencyMs = rpcMode.getPollingFrequencyMs();
    this.resendFrequencyWhenNoReplyMs = rpcMode.getResendFrequencyWhenNoReplyMs();
    this.noReplay = true;
    this.autoRemove = false;
  }

  protected abstract SorobanEndpoint newEndpointReply(I request, Bip47Encrypter encrypter);

  @Override
  public SorobanEndpoint getEndpointReply(I request, Bip47Encrypter encrypter) {
    SorobanEndpoint endpointReply = newEndpointReply(request, encrypter);

    // preserve config
    endpointReply.setPollingFrequencyMs(pollingFrequencyMs);
    endpointReply.setResendFrequencyWhenNoReplyMs(resendFrequencyWhenNoReplyMs);
    return endpointReply;
  }

  protected abstract Pair<String, M> newEntry(S payload) throws Exception;

  protected abstract String entryToRaw(Pair<String, M> entry) throws Exception;

  protected abstract Pair<String, M> rawToEntry(String rawEntry) throws Exception;

  protected abstract I newEntry(Pair<String, M> entry, String rawEntry);

  protected abstract String getRawEntry(I entry);

  protected abstract F newFilterBuilder();

  protected F createFilter(Consumer<F> f) {
    F filter = newFilterBuilder();
    f.accept(filter);
    return filter;
  }

  protected Pair<String, M> encryptOnSend(
      Bip47Encrypter encrypter, Pair<String, M> entry, Object initialPayload) throws Exception {
    if (encryptTo != null) {
      // encrypt to partner
      /*if (log.isDebugEnabled()) {
        log.debug("[" + path + "] encryptOnSend: " + encryptTo);
      }*/
      entry = encryptTo(encrypter, entry, initialPayload, encryptTo);
    } /* else {
        if (log.isDebugEnabled()) {
          log.debug("[" + path + "] encryptOnSend: not encrypted");
        }
      }*/
    return entry;
  }

  protected Pair<String, M> decryptOnReceive(Bip47Encrypter encrypter, Pair<String, M> entry)
      throws Exception {
    if (decryptFrom != null) {
      // decrypt from partner
      /*if (log.isDebugEnabled()) {
        log.debug("[" + path + "] decryptOnReceive: " + decryptFrom);
      }*/
      entry = decryptFrom(encrypter, entry, decryptFrom);
    } /* else {
        if (log.isDebugEnabled()) {
          log.debug("[" + path + "] decryptOnReceive: not encrypted");
        }
      }*/
    return entry;
  }

  protected Pair<String, M> encryptTo(
      Bip47Encrypter encrypter, Pair<String, M> entry, Object initialPayload, PaymentCode encryptTo)
      throws Exception {
    String payload = entry.getLeft();
    payload = new SorobanEncrypter(encryptTo).encrypt(encrypter, payload);
    return Pair.of(payload, entry.getRight());
  }

  protected Pair<String, M> decryptFrom(
      Bip47Encrypter encrypter, Pair<String, M> entry, PaymentCode decryptFrom) throws Exception {
    String payload = entry.getLeft();
    payload = new SorobanEncrypter(decryptFrom).decrypt(encrypter, payload);
    return Pair.of(payload, entry.getRight());
  }

  protected void setEncryptReply(
      AbstractSorobanEndpoint<I, S, M, F> endpointRequest, I request, Bip47Encrypter encrypter) {
    if (endpointRequest.encryptTo != null) {
      this.decryptFrom = endpointRequest.encryptTo;
    } else if (endpointRequest.decryptFrom != null) {
      this.encryptTo = endpointRequest.decryptFrom;
    }
    /*if (log.isDebugEnabled()) {
      log.debug(
          "setEncryptReply: encryptTo = "
              + (encryptTo != null ? encryptTo : "null")
              + ", decryptFrom="
              + (decryptFrom != null ? decryptFrom : "null"));
    }*/
  }

  protected Pair<String, M> applyWrappersOnSend(
      Bip47Encrypter encrypter, Pair<String, M> entry, Object initialPayload) throws Exception {
    String payload = entry.getLeft();
    for (SorobanWrapperString wrapper : wrappers) {
      payload = wrapper.onSend(encrypter, payload, initialPayload);
    }
    entry = Pair.of(payload, entry.getRight());
    entry = encryptOnSend(encrypter, entry, initialPayload);
    return entry;
  }

  protected Pair<String, M> applyWrappersOnReceive(Bip47Encrypter encrypter, Pair<String, M> entry)
      throws Exception {
    entry = decryptOnReceive(encrypter, entry);
    String payload = entry.getLeft();
    for (SorobanWrapperString wrapper : wrappers) {
      payload = wrapper.onReceive(encrypter, payload);
    }
    return Pair.of(payload, entry.getRight());
  }

  @Override
  public Completable send(SorobanClient sorobanClient, S payload) {
    return Completable.fromSingle(sendSingle(sorobanClient, payload));
  }

  @Override
  public Single<I> sendSingle(SorobanClient sorobanClient, S payload) {
    try {
      Pair<String, M> entry = newEntry(payload);
      return sendSingle(sorobanClient, entry, payload);
    } catch (Exception e) {
      log.error("newEntry() failed", e);
      return Single.error(e);
    }
  }

  protected Single<I> sendSingle(
      SorobanClient sorobanClient, Pair<String, M> entry, Object initialPayload) {
    try {
      // apply wrappers
      Bip47Encrypter encrypter = sorobanClient.getEncrypter();
      String clearPayload = entry.getLeft();
      Pair<String, M> entryWithMetadata = Pair.of(entry.getLeft(), entry.getRight());
      entryWithMetadata = applyWrappersOnSend(encrypter, entryWithMetadata, initialPayload);
      String unencryptedPayload = entryWithMetadata.getLeft();
      String rawEntry = entryToRaw(entryWithMetadata);

      // result must include clear payload and full metadata, to use it with getEndpointReply()
      Pair<String, M> entryClearPayloadWithMetadata =
          Pair.of(clearPayload, entryWithMetadata.getRight());
      I result = newEntry(entryClearPayloadWithMetadata, rawEntry);

      // send String
      String dir = getDir();
      return sorobanClient
          .getRpcClient()
          .directoryAdd(dir, rawEntry, rpcMode)
          .toSingle(
              () -> {
                // log into toSingle() to void false-logging when not subscribed
                if (log.isDebugEnabled()) {
                  boolean encrypted = !clearPayload.equals(unencryptedPayload);
                  String debug =
                      "=> ADD sorobanDir="
                          + dir
                          + " "
                          + logEntry(entryClearPayloadWithMetadata)
                          + "\n, encrypted="
                          + encrypted;
                  if (log.isTraceEnabled()) {
                    debug += ", rawEntry=" + rawEntry;
                  }
                  log.debug(debug);
                }
                return result;
              });
    } catch (Exception e) {
      log.error("sendSingle() failed", e);
      return Single.error(e);
    }
  }

  @Override
  public Completable remove(SorobanClient sorobanClient, I entry) {
    String rawEntry = getRawEntry(entry);
    return removeRaw(sorobanClient, rawEntry);
  }

  @Override
  public Completable removeRaw(SorobanClient sorobanClient, String rawEntry) {
    return sorobanClient.getRpcClient().directoryRemove(getDir(), rawEntry);
  }

  @Override
  public Single<Optional<I>> findAny(SorobanClient sorobanClient) {
    return findAny(sorobanClient, null);
  }

  public Single<Optional<I>> findAny(SorobanClient sorobanClient, Consumer<F> filterBuilderOrNull) {
    // if (filterBuilderOrNull != null) {
    // list all payloads to apply sort criterias
    return getList(sorobanClient, filterBuilderOrNull)
        .map(
            list -> {
              if (list.isEmpty()) {
                // no value found
                return Optional.empty();
              }
              I lastItem = list.get(list.size() - 1);
              if (log.isTraceEnabled()) {
                log.trace("findAny sorobanDir=" + getDir() + " -> result=" + lastItem);
              }
              return Optional.of(lastItem);
            });
    /*}

    // optimized by stopping on first payload
    Bip47Encrypter encrypter = sorobanClient.getEncrypter();
    return sorobanClient
        .getRpcClient()
        .directoryValues(getDir())
        .map(
            entries -> {
              // TODO take last item
              ArrayUtils.reverse(entries);
              for (String rawEntry : entries) {
                // loop until valid entry
                try {
                  return Optional.of(readItem(encrypter, rawEntry, sorobanClient));
                } catch (Exception e) {
                  // skip invalid payload
                }
              }
              return Optional.empty();
            });*/
  }

  @Override
  public I loopWaitAny(RpcSession rpcSession, long timeoutMs) throws Exception {
    return loopWaitAny(rpcSession, timeoutMs, null);
  }

  @Override
  public I loopWaitAny(RpcSession rpcSession, long timeoutMs, Consumer<F> filterBuilderOrNull)
      throws Exception {
    return rpcSession.loopUntilSuccess(
        sorobanClient -> asyncUtil.blockingGet(findAny(sorobanClient, filterBuilderOrNull)),
        getPollingFrequencyMs(),
        timeoutMs);
  }

  public Single<List<I>> doGetList(SorobanClient sorobanClient) {
    Bip47Encrypter encrypter = sorobanClient.getEncrypter();
    return sorobanClient
        .getRpcClient()
        .directoryValues(getDir())
        .map(entries -> readList(encrypter, entries, sorobanClient));
  }

  protected List<I> readList(
      Bip47Encrypter encrypter, String[] entries, SorobanClient sorobanClient) {
    List<I> items = readItems(encrypter, entries, sorobanClient);
    return new LinkedList<>(items);
  }

  protected List<I> readItems(
      Bip47Encrypter encrypter, String[] entries, SorobanClient sorobanClient) {
    return Arrays.stream(entries)
        .map(
            entry -> {
              try {
                return readItem(encrypter, entry, sorobanClient);
              } catch (Exception e) {
                return null; // skip invalid payload
              }
            })
        .filter(e -> e != null)
        .collect(Collectors.toList());
  }

  protected String logEntry(Pair<String, M> entry) {
    return "payload:" + entry.getLeft() + ", metadata:" + entry.getRight();
  }

  protected I readItem(Bip47Encrypter encrypter, String rawEntry, SorobanClient sorobanClient)
      throws Exception {
    try {
      // apply wrappers
      Pair<String, M> entry = rawToEntry(rawEntry);
      String rawPayload = entry.getLeft();
      Pair<String, M> readEntry = applyWrappersOnReceive(encrypter, entry);
      I item = newEntry(readEntry, rawEntry);
      if (log.isTraceEnabled()) {
        boolean encrypted = !readEntry.getLeft().equals(rawPayload);
        String debug =
            "<= READ sorobanDir="
                + getDir()
                + ": "
                + logEntry(readEntry)
                + "\n, encrypted="
                + encrypted;
        if (log.isTraceEnabled()) {
          debug += ", rawEntry=" + rawEntry;
        }
        log.trace(debug);
      }
      if (autoRemove) {
        // delete in background
        removeRaw(sorobanClient, rawEntry).subscribe();
      }
      return item;
    } catch (Exception e) {
      log.warn(
          "<= INVALID PAYLOAD, skipping... sorobanDir="
              + getDir()
              + " ("
              + e.getMessage()
              + "):\n"
              + rawEntry);
      throw e;
    }
  }

  @Override
  public Single<List<I>> getList(SorobanClient sorobanClient) {
    return getList(sorobanClient, null);
  }

  @Override
  public Single<List<I>> getList(SorobanClient sorobanClient, Consumer<F> filterBuilderOrNull) {
    Single<List<I>> list = doGetList(sorobanClient);

    // filter
    F filter = filterBuilderOrNull != null ? createFilter(filterBuilderOrNull) : null;

    list =
        list.map(
            l -> {
              Stream<I> stream = l.stream();
              if (filter != null) {
                stream = filter.applyFilter(l.stream());
              }
              if (noReplay) {
                stream =
                    applyFilterNoReplay(stream)
                        .map(
                            i -> {
                              onReadNoReplay(i);
                              return i;
                            });
              }
              List<I> filteredList = stream.collect(Collectors.toList());
              if (log.isDebugEnabled() && l.size() > 0 || log.isTraceEnabled()) {
                log.debug(
                    "<= LIST("
                        + l.size()
                        + " -> "
                        + filteredList.size()
                        + ") sorobanDir="
                        + getDir());
                for (I item : filteredList) {
                  log.debug(" * " + item);
                }
              }
              return filteredList;
            });
    return list;
  }

  protected Stream<I> applyFilterNoReplay(Stream<I> stream) {
    return stream; // override
  }

  protected void onReadNoReplay(I item) {
    // override
  }

  protected RpcMode getRpcMode() {
    return rpcMode;
  }

  public RpcMode getReplyRpcMode() {
    return replyRpcMode;
  }

  public void setReplyRpcMode(RpcMode replyRpcMode) {
    this.replyRpcMode = replyRpcMode;
  }

  @Override
  public int getExpirationMs() {
    return rpcMode.getExpirationMs();
  }

  protected int getDefaultLoopTimeoutMs() {
    return 2 * getExpirationMs();
  }

  @Override
  public int getPollingFrequencyMs() {
    return pollingFrequencyMs;
  }

  @Override
  public void setPollingFrequencyMs(int pollingFrequencyMs) {
    this.pollingFrequencyMs = pollingFrequencyMs;
  }

  @Override
  public int getResendFrequencyWhenNoReplyMs() {
    return resendFrequencyWhenNoReplyMs;
  }

  @Override
  public void setResendFrequencyWhenNoReplyMs(int resendFrequencyWhenNoReplyMs) {
    this.resendFrequencyWhenNoReplyMs = resendFrequencyWhenNoReplyMs;
  }

  @Override
  public String getDir() {
    return dir;
  }

  public PaymentCode getEncryptTo() {
    return encryptTo;
  }

  public PaymentCode getDecryptFrom() {
    return decryptFrom;
  }

  /*public AbstractSorobanEndpoint<I, S, M, F> setEncryptOneToOne(
      PaymentCode encryptTo, PaymentCode decryptFrom) {
    this.encryptTo = encryptTo;
    this.decryptFrom = decryptFrom;
    return this;
  }*/

  public AbstractSorobanEndpoint<I, S, M, F> setEncryptTo(PaymentCode encryptTo) {
    this.encryptTo = encryptTo;
    return this;
  }

  public AbstractSorobanEndpoint<I, S, M, F> setDecryptFrom(PaymentCode decryptFrom) {
    this.decryptFrom = decryptFrom;
    return this;
  }

  protected List<SorobanWrapperString> getWrappers() {
    return wrappers;
  }

  public boolean isNoReplay() {
    return noReplay;
  }

  @Override
  public void setNoReplay(boolean noReplay) {
    this.noReplay = noReplay;
  }

  public void setAutoRemove(boolean autoRemove) {
    this.autoRemove = autoRemove;
  }

  @Override
  public String toString() {
    return "{" + "dir='" + dir + '\'' + '}';
  }
}
