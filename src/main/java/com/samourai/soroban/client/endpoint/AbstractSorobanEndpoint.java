package com.samourai.soroban.client.endpoint;

import com.samourai.soroban.client.AckResponse;
import com.samourai.soroban.client.SorobanClient;
import com.samourai.soroban.client.SorobanPayloadable;
import com.samourai.soroban.client.endpoint.wrapper.SorobanWrapperString;
import com.samourai.soroban.client.rpc.RpcClient;
import com.samourai.soroban.client.rpc.RpcMode;
import com.samourai.soroban.client.rpc.RpcSession;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSorobanEndpoint<I, L, E> implements SorobanEndpoint<I, L> {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private SorobanApp app;
  private String path;
  private RpcMode rpcMode;
  private SorobanWrapperString[] wrappers;

  public AbstractSorobanEndpoint(
      SorobanApp app, String path, RpcMode rpcMode, SorobanWrapperString[] wrappers) {
    this.app = app;
    this.path = path;
    this.rpcMode = rpcMode;
    this.wrappers = wrappers;
  }

  protected abstract E newEntry(String payload) throws Exception;

  protected abstract String toPayload(E entry) throws Exception;

  protected abstract E readEntry(Bip47Encrypter encrypter, String entry) throws Exception;

  protected abstract L newList(List<I> items);

  protected abstract I newEntry(E entry, String rawEntry);

  protected abstract String getRawEntry(I entry);

  protected String applyWrappersForSend(
      Bip47Encrypter encrypter, E entryObject, Object initialPayload) throws Exception {
    String entry = toPayload(entryObject);
    for (SorobanWrapperString wrapper : wrappers) {
      entry = wrapper.onSend(encrypter, entry, initialPayload);
    }
    return entry;
  }

  @Override
  public Completable send(SorobanClient sorobanClient, String payload) throws Exception {
    return Completable.fromSingle(sendSingle(sorobanClient, payload));
  }

  @Override
  public Completable send(SorobanClient sorobanClient, SorobanPayloadable sorobanPayloadable)
      throws Exception {
    return Completable.fromSingle(sendSingle(sorobanClient, sorobanPayloadable));
  }

  @Override
  public Completable sendAck(SorobanClient sorobanClient) throws Exception {
    return send(sorobanClient, new AckResponse());
  }

  protected Single<I> sendSingle(SorobanClient sorobanClient, String payload) throws Exception {
    E entry = newEntry(payload);
    return sendSingle(sorobanClient, entry, payload);
  }

  protected Single<I> sendSingle(SorobanClient sorobanClient, SorobanPayloadable sorobanPayloadable)
      throws Exception {
    E entry = newEntry(sorobanPayloadable.toPayload());
    return sendSingle(sorobanClient, entry, sorobanPayloadable);
  }

  protected Single<I> sendSingle(SorobanClient sorobanClient, E entryObject, Object initialPayload)
      throws Exception {
    // apply wrappers
    Bip47Encrypter encrypter = sorobanClient.getEncrypter();
    String clearLog = entryObject.toString();
    String entry = applyWrappersForSend(encrypter, entryObject, initialPayload);

    // send String
    String dir = getDir();
    if (log.isDebugEnabled()) {
      log.debug(" -> " + RpcClient.shortDirectory(dir) + " " + entry + "\n(was: " + clearLog + ")");
    }
    return sorobanClient
        .getRpcClient()
        .directoryAdd(dir, entry, rpcMode)
        .toSingle(() -> newEntry(entryObject, entry));
  }

  @Override
  public Completable delete(SorobanClient sorobanClient, I entry) throws Exception {
    String rawEntry = getRawEntry(entry);
    return sorobanClient.getRpcClient().directoryRemove(getDir(), rawEntry);
  }

  @Override
  public Single<Optional<I>> getNext(SorobanClient sorobanClient) throws Exception {
    Bip47Encrypter encrypter = sorobanClient.getEncrypter();
    return sorobanClient
        .getRpcClient()
        .directoryValues(getDir())
        .map(
            entries -> {
              for (String entry : entries) {
                // loop until valid entry
                try {
                  return Optional.of(readItem(encrypter, entry));
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
        .map(entry -> readItem(encrypter, entry));
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
        .map(entries -> readList(encrypter, entries));
  }

  protected L readList(Bip47Encrypter encrypter, String[] entries) {
    List<I> items = readItems(encrypter, entries);
    if (log.isDebugEnabled()) {
      log.debug(" <- " + getDir() + ": " + items.size() + " entries");
    }
    return newList(items);
  }

  protected List<I> readItems(Bip47Encrypter encrypter, String[] entries) {
    return Arrays.stream(entries)
        .map(
            entry -> {
              try {
                return readItem(encrypter, entry);
              } catch (Exception e) {
                log.warn("Skipping invalid SorobanPayload", e);
                return null;
              }
            })
        .filter(e -> e != null)
        .collect(Collectors.toList());
  }

  protected I readItem(Bip47Encrypter encrypter, String entry) throws Exception {
    try {
      String rawEntry = entry;

      // apply wrappers
      for (SorobanWrapperString wrapper : wrappers) {
        entry = wrapper.onReceive(encrypter, entry);
      }

      E readEntry = readEntry(encrypter, entry);
      I item = newEntry(readEntry, rawEntry);
      if (log.isDebugEnabled()) {
        log.debug(" <- " + getDir() + ": " + entry);
      }
      return item;
    } catch (Exception e) {
      log.warn(" <- " + getDir() + ": " + entry + ": INVALID: " + e.getMessage());
      throw e;
    }
  }

  // REPLY

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
}
