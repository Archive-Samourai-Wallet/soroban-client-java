package com.samourai.soroban.client.rpc;

import com.samourai.soroban.client.RpcWallet;
import com.samourai.soroban.client.SorobanClient;
import com.samourai.soroban.client.SorobanServerDex;
import com.samourai.soroban.client.dialog.RpcDialog;
import com.samourai.wallet.api.backend.beans.HttpException;
import com.samourai.wallet.bip47.BIP47UtilGeneric;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import com.samourai.wallet.crypto.CryptoUtil;
import com.samourai.wallet.util.AsyncUtil;
import com.samourai.wallet.util.CallbackWithArg;
import com.samourai.wallet.util.RandomUtil;
import com.samourai.wallet.util.urlStatus.UpStatusPool;
import io.reactivex.Single;
import io.reactivex.functions.BiFunction;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcSession {
  private static final Logger log = LoggerFactory.getLogger(RpcSession.class.getName());
  private static final int DOWN_DELAY_MS = 300000; // 5min
  protected static final int RETRY_DELAY_MS = 1000;
  private static final UpStatusPool upStatusPool = new UpStatusPool(DOWN_DELAY_MS);
  private static final AsyncUtil asyncUtil = AsyncUtil.getInstance();

  private RpcClientService rpcClientService;
  private CryptoUtil cryptoUtil;
  private BIP47UtilGeneric bip47Util;
  private ECKey authenticationKey;
  private RpcWallet rpcWallet;
  private boolean done;

  public RpcSession(
      RpcClientService rpcClientService,
      CryptoUtil cryptoUtil,
      BIP47UtilGeneric bip47Util,
      RpcWallet rpcWallet) {
    this.rpcClientService = rpcClientService;
    this.cryptoUtil = cryptoUtil;
    this.bip47Util = bip47Util;
    this.rpcWallet = rpcWallet;
    this.done = false;
  }

  public void setAuthenticationKey(ECKey authenticationKey) {
    this.authenticationKey = authenticationKey;
  }

  protected Collection<String> getServerUrlsUp() {
    NetworkParameters params = rpcClientService.getParams();
    boolean onion = rpcClientService.isOnion();
    Collection<String> serverUrls = SorobanServerDex.get(params).getServerUrls(onion);
    Collection<String> serverUrlsUp = upStatusPool.filterNotDown(serverUrls);
    if (serverUrlsUp.isEmpty()) {
      // retry when all down
      log.warn("All SorobanServerDex appears to be down, retrying...");
      return serverUrls;
    }
    return serverUrlsUp;
  }

  public <R> R withRpcServerUrl(CallbackWithArg<String, R> callable) throws Exception {
    int attempts = 0;

    // shuffle serverUrls
    List<String> serverUrls = new LinkedList<>(getServerUrlsUp());
    if (serverUrls.isEmpty()) {
      throw new HttpException("RPC failed: no SorobanServerDex available");
    }
    int nbServers = serverUrls.size();
    RandomUtil.getInstance().shuffle(serverUrls);
    Exception lastException = null;

    while (!serverUrls.isEmpty()) {
      // retry with each serverUrl
      String serverUrl = serverUrls.remove(0);
      try {
        R result = withRpcServerUrl(callable, serverUrl);
        if (attempts > 0) {
          attempts++;
          if (log.isDebugEnabled()) {
            log.debug("RPC success: attempt " + attempts + "/" + nbServers + ", url=" + serverUrl);
          }
        }
        return result;
      } catch (HttpException e) {
        attempts++;
        lastException = e;
        log.warn("RPC failed: attempt " + attempts + "/" + nbServers + ", url=" + serverUrl);
      } catch (Throwable e) {
        throw e; // abort on non-http exception
      }
    }
    throw lastException;
  }

  public <R> R withRpcServerUrl(CallbackWithArg<String, R> callable, String serverUrlForced)
      throws Exception {
    if (serverUrlForced == null) {
      return withRpcServerUrl(callable);
    }
    try {
      R result = callable.apply(serverUrlForced + RpcClient.ENDPOINT_RPC);
      upStatusPool.setStatusUp(serverUrlForced);
      return result;
    } catch (TimeoutException e) {
      upStatusPool.setStatusDown(serverUrlForced, e);
      throw e;
    } catch (Throwable e) {
      throw e;
    }
  }

  public <R> R withRpcClient(CallbackWithArg<RpcClient, R> callable) throws Exception {
    return withRpcClient(callable, null);
  }

  public <R> R withRpcClient(CallbackWithArg<RpcClient, R> callable, String serverUrlForced)
      throws Exception {
    return withRpcServerUrl(
        serverUrl -> {
          RpcClient rpcClient = rpcClientService.createRpcClient(serverUrl);
          if (authenticationKey != null) {
            rpcClient.setAuthenticationKey(authenticationKey);
          }
          return callable.apply(rpcClient);
        },
        serverUrlForced);
  }

  public <R> R withSorobanClient(CallbackWithArg<SorobanClient, R> callable, String serverUrlForced)
      throws Exception {
    Bip47Encrypter encrypter = rpcWallet.getBip47Encrypter();
    return withRpcClient(
        rpcClient -> {
          SorobanClient sorobanClient = new SorobanClient(rpcClient, encrypter);
          return callable.apply(sorobanClient);
        },
        serverUrlForced);
  }

  public <R> R withSorobanClient(CallbackWithArg<SorobanClient, R> callable) throws Exception {
    return withSorobanClient(callable, null);
  }

  public RpcDialog createRpcDialog(String directory) throws Exception {
    return new RpcDialog(directory, this);
  }

  public RpcWallet getRpcWallet() {
    return rpcWallet;
  }

  public static UpStatusPool getUpStatusPool() {
    return upStatusPool;
  }

  public <T> Single<T> loopUntil(BiFunction<SorobanClient, String, Single<T>> getMessage) {
    CallbackWithArg<SorobanClient, Single<String>> getRequestId = sorobanClient -> null;
    return loopUntil(getRequestId, getMessage);
  }

  public <T> Single<T> loopUntil(
      CallbackWithArg<SorobanClient, Single<String>> getRequestId,
      BiFunction<SorobanClient, String, Single<T>> getMessage) {
    Single<T> loop =
        Single.fromCallable(
            () -> {
              while (true) {
                throwIfDone();
                try {
                  // use fresh soroban node
                  return asyncUtil.blockingGet(
                      withSorobanClient(
                          sorobanClient -> {
                            // do stuff
                            String requestId =
                                asyncUtil.blockingGet(getRequestId.apply(sorobanClient));

                            // wait for response
                            return getMessage.apply(sorobanClient, requestId);
                          }));
                } catch (NoValueRpcException e) {
                  // no response received yet, continue looping
                  synchronized (this) {
                    try {
                      wait(RETRY_DELAY_MS);
                    } catch (InterruptedException ee) {
                    }
                  }
                }
              }
            });
    return loop;
  }

  public <R> Single<R> directoryValueWait(
      final String directory, final long timeoutMs, BiFunction<SorobanClient, String, R> onSuccess)
      throws Exception {
    long timeOut = System.currentTimeMillis() + timeoutMs;
    while (System.currentTimeMillis() < timeOut && !done) {
      try {
        return Single.just(
            asyncUtil.blockingGet(
                withSorobanClient(
                    sorobanClient ->
                        sorobanClient
                            .getRpcClient()
                            .directoryValue(directory)
                            .map(value -> onSuccess.apply(sorobanClient, value)))));
      } catch (NoValueRpcException e) {
        synchronized (this) {
          try {
            wait(RETRY_DELAY_MS);
          } catch (InterruptedException ee) {
          }
        }
      }
    }
    throw new NoValueRpcException();
  }

  public Single<String> directoryValueWait(final String directory, final long timeoutMs)
      throws Exception {
    return directoryValueWait(directory, timeoutMs, (sorobanClient, value) -> value);
  }

  public Single<String> directoryValueWaitAndRemove(final String directory, final long timeoutMs)
      throws Exception {
    return directoryValueWait(
        directory,
        timeoutMs,
        (sorobanClient, value) -> {
          sorobanClient.getRpcClient().directoryRemove(directory, value);
          return value;
        });
  }

  public void throwIfDone() throws Exception {
    if (done) {
      throw new Exception("Terminating");
    }
  }

  public void exit() {
    done = true;
  }
}
