package com.samourai.soroban.client.endpoint;

import com.samourai.soroban.client.SorobanClient;
import com.samourai.soroban.client.endpoint.meta.SorobanFilter;
import com.samourai.soroban.client.endpoint.wrapper.SorobanWrapperString;
import com.samourai.soroban.client.rpc.RpcClient;
import com.samourai.soroban.client.rpc.RpcMode;
import com.samourai.soroban.client.rpc.RpcSession;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.util.AsyncUtil;
import com.samourai.wallet.util.Pair;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSorobanEndpoint<I, S, M, F extends SorobanFilter<I>>
    implements SorobanEndpoint<I, S, F> {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  protected static final AsyncUtil asyncUtil = AsyncUtil.getInstance();

  private SorobanApp app;
  private String path;
  private RpcMode rpcMode;
  private List<SorobanWrapperString> wrappers;
  private boolean autoRemove;
  private PaymentCode encryptTo; // may be null
  private PaymentCode decryptFrom; // may be null
  private int pollingFrequencyMs;
  private int resendFrequencyWhenNoReplyMs;

  public AbstractSorobanEndpoint(
      SorobanApp app, String path, RpcMode rpcMode, SorobanWrapperString[] wrappers) {
    this.app = app;
    this.path = path;
    this.rpcMode = rpcMode;
    this.wrappers = Arrays.asList(wrappers);
    this.autoRemove = false;
    this.encryptTo = null;
    this.decryptFrom = null;
    this.pollingFrequencyMs = rpcMode.getPollingFrequencyMs();
    this.resendFrequencyWhenNoReplyMs = rpcMode.getResendFrequencyWhenNoReplyMs();
  }

  protected abstract SorobanEndpoint newEndpointReply(I request, Bip47Encrypter encrypter);

  @Override
  public SorobanEndpoint getEndpointReply(I request, Bip47Encrypter encrypter) {
    SorobanEndpoint endpointReply = newEndpointReply(request, encrypter);
    endpointReply.setAutoRemove(true);

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

  protected F createFilter(Consumer<F> filterBuilder) {
    F filter = newFilterBuilder();
    filterBuilder.accept(filter);
    return filter;
  }

  /*protected List<I> applyFilter(Consumer<F> filterBuilderOrNull, List<I> list) {
    if (filterBuilderOrNull == null) {
      return list;
    }
    F filter = createFilter(filterBuilderOrNull);
    return filter.applyFilter(list.stream()).collect(Collectors.toList());
  }*/

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
      String rawEntry = entryToRaw(entryWithMetadata);

      // result must include clear payload and full metadata, to use it with getEndpointReply()
      Pair<String, M> entryClearPayloadWithMetadata =
          Pair.of(clearPayload, entryWithMetadata.getRight());
      I result = newEntry(entryClearPayloadWithMetadata, rawEntry);

      // send String
      String dir = getDir();
      if (log.isDebugEnabled()) {
        boolean encrypted = !clearPayload.equals(entryWithMetadata.getLeft());
        log.debug(
            "=> ADD "
                + RpcClient.shortDirectory(dir)
                + ": "
                + logEntry(entryClearPayloadWithMetadata)
                + "\n, encrypted="
                + encrypted
                + ", rawEntry="
                + rawEntry);
      }
      return sorobanClient
          .getRpcClient()
          .directoryAdd(dir, rawEntry, rpcMode)
          .toSingle(() -> result);
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
    if (filterBuilderOrNull != null) {
      // list all payloads to apply sort criterias
      return getList(sorobanClient, filterBuilderOrNull)
          .map(list -> list.isEmpty() ? Optional.empty() : Optional.of(list.iterator().next()));
    }

    // optimized by stopping on first payload
    Bip47Encrypter encrypter = sorobanClient.getEncrypter();
    return sorobanClient
        .getRpcClient()
        .directoryValues(getDir())
        .map(
            entries -> {
              for (String rawEntry : entries) {
                // loop until valid entry
                try {
                  return Optional.of(readItem(encrypter, rawEntry, sorobanClient));
                } catch (Exception e) {
                  // skip invalid payload
                }
              }
              return Optional.empty();
            });
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

  @Override
  public Single<List<I>> getList(SorobanClient sorobanClient) {
    Bip47Encrypter encrypter = sorobanClient.getEncrypter();
    return sorobanClient
        .getRpcClient()
        .directoryValues(getDir())
        .map(entries -> readList(encrypter, entries, sorobanClient));
  }

  protected List<I> readList(
      Bip47Encrypter encrypter, String[] entries, SorobanClient sorobanClient) {
    List<I> items = readItems(encrypter, entries, sorobanClient);
    if (log.isDebugEnabled()) {
      log.debug("<= READLIST(" + items.size() + ") " + getDir());
    }
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
    return "{payload:" + entry.getLeft() + ", metadata:" + entry.getRight() + "}";
  }

  protected I readItem(Bip47Encrypter encrypter, String rawEntry, SorobanClient sorobanClient)
      throws Exception {
    try {
      // apply wrappers
      Pair<String, M> entry = rawToEntry(rawEntry);
      String rawPayload = entry.getLeft();
      Pair<String, M> readEntry = applyWrappersOnReceive(encrypter, entry);
      I item = newEntry(readEntry, rawEntry);
      if (log.isDebugEnabled()) {
        boolean encrypted = !readEntry.getLeft().equals(rawPayload);
        log.debug(
            "<= READ "
                + getDir()
                + ": "
                + logEntry(readEntry)
                + "\n, encrypted="
                + encrypted
                + ", rawEntry="
                + rawEntry);
      }
      if (autoRemove) {
        // delete in background
        removeRaw(sorobanClient, rawEntry).subscribe();
      }
      return item;
    } catch (Exception e) {
      log.warn(
          "<= INVALID PAYLOAD, skipping... "
              + getDir()
              + " ("
              + e.getMessage()
              + "):\n"
              + rawEntry);
      throw e;
    }
  }

  @Override
  public Single<List<I>> getList(SorobanClient sorobanClient, Consumer<F> filterBuilderOrNull) {
    Single<List<I>> list = getList(sorobanClient);

    // filter
    F filter = createFilter(filterBuilderOrNull);
    if (filterBuilderOrNull != null) {
      list = list.map(l -> filter.applyFilter(l.stream()).collect(Collectors.toList()));
    }
    return list;
  }

  public abstract String computeUniqueId(I entry);

  @Override
  public String getPathReply(I entry) {
    return getPath() + "/REPLY/" + computeUniqueId(entry);
  }

  @Override
  public SorobanApp getApp() {
    return app;
  }

  protected String getPath() {
    return path;
  }

  protected RpcMode getRpcMode() {
    return rpcMode;
  }

  @Override
  public int getExpirationMs() {
    return rpcMode.getExpirationMs();
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

  protected String getDir() {
    return app.getDir(path);
  }

  @Override
  public void setAutoRemove(boolean autoRemove) {
    this.autoRemove = autoRemove;
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

  @Override
  public String toString() {
    return "{" + "path='" + path + '\'' + '}';
  }
}
