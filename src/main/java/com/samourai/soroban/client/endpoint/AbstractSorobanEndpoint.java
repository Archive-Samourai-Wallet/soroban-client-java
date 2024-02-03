package com.samourai.soroban.client.endpoint;

import com.samourai.soroban.client.SorobanClient;
import com.samourai.soroban.client.endpoint.wrapper.SorobanWrapperString;
import com.samourai.soroban.client.rpc.RpcClient;
import com.samourai.soroban.client.rpc.RpcMode;
import com.samourai.soroban.client.rpc.RpcSession;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import com.samourai.wallet.util.AsyncUtil;
import com.samourai.wallet.util.Pair;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSorobanEndpoint<I, L extends List<I>, S, M>
    implements SorobanEndpoint<I, L, S> {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  protected static final AsyncUtil asyncUtil = AsyncUtil.getInstance();

  private SorobanApp app;
  private String path;
  private RpcMode rpcMode;
  private SorobanWrapperString[] wrappers;
  private boolean autoRemove;

  public AbstractSorobanEndpoint(
      SorobanApp app, String path, RpcMode rpcMode, SorobanWrapperString[] wrappers) {
    this.app = app;
    this.path = path;
    this.rpcMode = rpcMode;
    this.wrappers = wrappers;
    this.autoRemove = false;
  }

  protected abstract Pair<String, M> newEntry(S payload) throws Exception;

  protected abstract String entryToRaw(Pair<String, M> entry) throws Exception;

  protected abstract Pair<String, M> rawToEntry(String rawEntry) throws Exception;

  protected abstract L newList(List<I> items);

  protected abstract I newEntry(Pair<String, M> entry, String rawEntry);

  protected abstract String getRawEntry(I entry);

  protected Pair<String, M> applyWrappersOnSend(
      Bip47Encrypter encrypter, Pair<String, M> entry, Object initialPayload) throws Exception {
    String payload = entry.getLeft();
    for (SorobanWrapperString wrapper : wrappers) {
      payload = wrapper.onSend(encrypter, payload, initialPayload);
    }
    return Pair.of(payload, entry.getRight());
  }

  protected Pair<String, M> applyWrappersOnReceive(Bip47Encrypter encrypter, Pair<String, M> entry)
      throws Exception {
    String payload = entry.getLeft();
    for (SorobanWrapperString wrapper : wrappers) {
      payload = wrapper.onReceive(encrypter, payload);
    }
    return Pair.of(payload, entry.getRight());
  }

  @Override
  public Completable send(SorobanClient sorobanClient, S payload) throws Exception {
    return Completable.fromSingle(sendSingle(sorobanClient, payload));
  }

  @Override
  public Single<I> sendSingle(SorobanClient sorobanClient, S payload) throws Exception {
    Pair<String, M> entry = newEntry(payload);
    return sendSingle(sorobanClient, entry, payload);
  }

  protected Single<I> sendSingle(
      SorobanClient sorobanClient, Pair<String, M> entry, Object initialPayload) throws Exception {
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
      log.debug(
          "=> ADD "
              + RpcClient.shortDirectory(dir)
              + " "
              + rawEntry
              + "\n(was: "
              + toString(entryClearPayloadWithMetadata)
              + ")");
    }
    return sorobanClient.getRpcClient().directoryAdd(dir, rawEntry, rpcMode).toSingle(() -> result);
  }

  @Override
  public Completable remove(SorobanClient sorobanClient, I entry) throws Exception {
    String rawEntry = getRawEntry(entry);
    return removeRaw(sorobanClient, rawEntry);
  }

  @Override
  public Completable removeRaw(SorobanClient sorobanClient, String rawEntry) throws Exception {
    return sorobanClient.getRpcClient().directoryRemove(getDir(), rawEntry);
  }

  @Override
  public Single<Optional<I>> getFirst(SorobanClient sorobanClient) throws Exception {
    return getFirst(sorobanClient, false);
  }

  @Override
  public Single<Optional<I>> getLast(SorobanClient sorobanClient) throws Exception {
    return getFirst(sorobanClient, true);
  }

  private Single<Optional<I>> getFirst(SorobanClient sorobanClient, boolean desc) throws Exception {
    Bip47Encrypter encrypter = sorobanClient.getEncrypter();
    return sorobanClient
        .getRpcClient()
        .directoryValues(getDir())
        .map(
            entries -> {
              if (desc) {
                ArrayUtils.reverse(entries);
              }
              for (String rawEntry : entries) {
                // loop until valid entry
                try {
                  return Optional.of(readItem(encrypter, rawEntry, sorobanClient));
                } catch (Exception e) {
                  log.warn("Skipping invalid payload: " + e.getMessage(), e);
                }
              }
              return Optional.empty();
            });
  }

  @Override
  public Single<I> waitNext(RpcSession rpcSession) {
    Bip47Encrypter encrypter = rpcSession.getRpcWallet().getBip47Encrypter();
    return rpcSession
        .directoryValueWaitAndRemove(getDir())
        .map(entry -> readItem(encrypter, entry, rpcSession.withSorobanClient(o -> o)));
  }

  protected long getLoopFrequencyMs() {
    switch (rpcMode) {
      case SHORT: // 30s
        return 30000;
      case NORMAL:
      default: // 60s
        return 60000;
    }
  }

  @Override
  public Single<L> getList(SorobanClient sorobanClient) throws Exception {
    Bip47Encrypter encrypter = sorobanClient.getEncrypter();
    return sorobanClient
        .getRpcClient()
        .directoryValues(getDir())
        .map(entries -> readList(encrypter, entries, sorobanClient));
  }

  protected L readList(Bip47Encrypter encrypter, String[] entries, SorobanClient sorobanClient) {
    List<I> items = readItems(encrypter, entries, sorobanClient);
    if (log.isDebugEnabled()) {
      log.debug("<= LIST " + getDir() + ": " + items.size() + " entries");
    }
    return newList(items);
  }

  protected List<I> readItems(
      Bip47Encrypter encrypter, String[] entries, SorobanClient sorobanClient) {
    return Arrays.stream(entries)
        .map(
            entry -> {
              try {
                return readItem(encrypter, entry, sorobanClient);
              } catch (Exception e) {
                log.warn("Skipping invalid SorobanPayload", e);
                return null;
              }
            })
        .filter(e -> e != null)
        .collect(Collectors.toList());
  }

  protected I readItem(Bip47Encrypter encrypter, String rawEntry, SorobanClient sorobanClient)
      throws Exception {
    try {
      // apply wrappers
      Pair<String, M> entry = rawToEntry(rawEntry);
      Pair<String, M> readEntry = applyWrappersOnReceive(encrypter, entry);
      I item = newEntry(readEntry, rawEntry);
      if (log.isDebugEnabled()) {
        log.debug("<= READ " + getDir() + ": " + toString(entry));
      }
      if (autoRemove) {
        // delete in background
        removeRaw(sorobanClient, rawEntry).subscribe();
      }
      return item;
    } catch (Exception e) {
      log.warn("<= READ " + getDir() + ": " + rawEntry + ": INVALID: " + e.getMessage());
      throw e;
    }
  }

  protected String toString(Pair<String, M> entry) {
    return "{payload:" + entry.getLeft() + ", metadata:" + entry.getRight() + "}";
  }

  public abstract String computeUniqueId(I entry);

  @Override
  public String getPathReply(I entry) {
    return getPath() + "/" + computeUniqueId(entry);
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

  protected String getDir() {
    return app.getDir(path);
  }

  @Override
  public void setAutoRemove(boolean autoRemove) {
    this.autoRemove = autoRemove;
  }

  @Override
  public String toString() {
    return "{" + "path='" + path + '\'' + '}';
  }
}
