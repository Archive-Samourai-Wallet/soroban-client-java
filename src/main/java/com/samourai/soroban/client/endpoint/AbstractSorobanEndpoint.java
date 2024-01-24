package com.samourai.soroban.client.endpoint;

import com.samourai.soroban.client.SorobanClient;
import com.samourai.soroban.client.SorobanPayload;
import com.samourai.soroban.client.rpc.RpcClient;
import com.samourai.soroban.client.rpc.RpcMode;
import com.samourai.soroban.client.wrapper.SorobanWrapper;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSorobanEndpoint<T, P extends SorobanPayload> {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private SorobanApp app;
  private String path;
  private RpcMode rpcMode;
  private SorobanWrapper[] wrappers;

  public AbstractSorobanEndpoint(
      SorobanApp app, String path, RpcMode rpcMode, SorobanWrapper... wrappers) {
    this.app = app;
    this.path = path;
    this.rpcMode = rpcMode;
    this.wrappers = wrappers;
  }

  protected abstract P adaptOnSend(String sender, T payload) throws Exception;

  protected abstract P adaptOnReceive(SorobanPayload entry) throws Exception;

  public Completable send(SorobanClient sorobanClient, T payload) throws Exception {
    // build payload
    String sender = sorobanClient.getEncrypter().getPaymentCode().toString();
    P p = adaptOnSend(sender, payload);
    SorobanPayload sorobanPayload = new SorobanPayloadImpl(p);

    // apply wrappers
    for (SorobanWrapper wrapper : wrappers) {
      sorobanPayload = wrapper.onSend(sorobanClient, sorobanPayload);
    }

    // send
    String dir = getDir();
    String entry = sorobanPayload.toPayload();
    if (log.isDebugEnabled()) {
      log.debug(" -> " + RpcClient.shortDirectory(dir) + " " + entry);
    }
    return sorobanClient.getRpcClient().directoryAdd(dir, entry, rpcMode);
  }

  public Single<P> getNext(SorobanClient sorobanClient) throws Exception {
    /*return sorobanClient
        .getRpcClient()
        .directoryValue(getDir())
        .map(entry -> read(sorobanClient, entry))
            .filter(entry -> entry != null);*/
    return getList(sorobanClient).map(list -> list.get(list.size()-1));
  }

  public Single<List<P>> getList(SorobanClient sorobanClient) throws Exception {
    return getList(sorobanClient, null);
  }

  public Single<List<P>> getList(SorobanClient sorobanClient, Predicate<P> filterOrNull) throws Exception {
    return sorobanClient
        .getRpcClient()
        .directoryValues(getDir())
        .map(
            entries ->
                Arrays.stream(entries)
                    .map(
                        entry -> {
                          try {
                            return read(sorobanClient, entry);
                          } catch (Exception e) {
                            return null;
                          }
                        })
                    .filter(payload -> {
                      if (payload != null) {
                        if (filterOrNull == null || filterOrNull.test(payload)) {
                          return true;
                        }
                      }
                      return false;
                    })
                    .collect(Collectors.toList()));
  }

  protected P read(SorobanClient sorobanClient, String entry) throws Exception {
    // build payload
    SorobanPayload sorobanPayload = new SorobanPayloadImpl(entry);

    // apply wrappers
    for (SorobanWrapper wrapper : wrappers) {
      sorobanPayload = wrapper.onReceive(sorobanClient, sorobanPayload);
    }
    P result = adaptOnReceive(sorobanPayload);
    return result;
  }

  public SorobanApp getApp() {
    return app;
  }

  public String getDir() {
    return app.getDir(path);
  }
}
