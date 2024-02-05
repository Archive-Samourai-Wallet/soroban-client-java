package com.samourai.soroban.client.rpc;

import com.samourai.soroban.client.RpcWallet;
import com.samourai.soroban.client.SorobanClient;
import com.samourai.soroban.client.SorobanServerDex;
import com.samourai.soroban.client.dialog.RpcDialog;
import com.samourai.wallet.api.backend.beans.HttpException;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import com.samourai.wallet.util.AsyncUtil;
import com.samourai.wallet.util.CallbackWithArg;
import com.samourai.wallet.util.RandomUtil;
import com.samourai.wallet.util.urlStatus.UpStatusPool;
import io.reactivex.Single;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcSession {
  private static final Logger log = LoggerFactory.getLogger(RpcSession.class.getName());
  private static final int DOWN_EXPIRATION_DELAY_MS = 600000; // 10min
  private static final UpStatusPool upStatusPool = new UpStatusPool(DOWN_EXPIRATION_DELAY_MS);
  private static final AsyncUtil asyncUtil = AsyncUtil.getInstance();

  private RpcClientService rpcClientService;
  private ECKey authenticationKey;
  private RpcWallet rpcWallet;
  private boolean done;

  public RpcSession(RpcSession rpcSession) {
    this(rpcSession.rpcClientService, rpcSession.rpcWallet);
  }

  public RpcSession(RpcClientService rpcClientService, RpcWallet rpcWallet) {
    this.rpcClientService = rpcClientService;
    this.rpcWallet = rpcWallet;
    this.done = false;
  }

  public void setAuthenticationKey(ECKey authenticationKey) {
    this.authenticationKey = authenticationKey;
  }

  public Collection<String> getServerUrlsUp() {
    boolean onion = rpcClientService.isOnion();
    return getServerUrlsUp(onion);
  }

  public Collection<String> getServerUrlsUp(boolean onion) {
    NetworkParameters params = rpcClientService.getParams();
    Collection<String> serverUrls = SorobanServerDex.get(params).getServerUrls(onion);
    Collection<String> serverUrlsUp = upStatusPool.filterNotDown(serverUrls);
    if (serverUrlsUp.isEmpty()) {
      // retry when all down
      log.warn("All SorobanServerDex appears to be down, retrying all...");
      upStatusPool.expireAll();
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
      return result;
    } /*catch (TimeoutException e) {
        upStatusPool.setStatusDown(serverUrlForced, e.getMessage());
        throw e;
      } */ catch (Throwable e) {
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

  public <R extends Single> R withSorobanClientSingle(
      CallbackWithArg<SorobanClient, R> callable, String serverUrlForced) {
    try {
      return withSorobanClient(callable, serverUrlForced);
    } catch (Exception e) {
      log.error("withSorobanClientSingle() failed", e);
      return (R) Single.error(e);
    }
  }

  public <R> R withSorobanClient(CallbackWithArg<SorobanClient, R> callable) throws Exception {
    return withSorobanClient(callable, null);
  }

  public <R extends Single> R withSorobanClientSingle(CallbackWithArg<SorobanClient, R> callable) {
    try {
      return withSorobanClient(callable, null);
    } catch (Exception e) {
      log.error("withSorobanClientSingle() failed", e);
      return (R) Single.error(e);
    }
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

  public <R> R loopUntilSuccess(
      CallbackWithArg<SorobanClient, Optional<R>> fetchValue,
      int pollingFrequencyMs,
      long timeoutMs)
      throws Exception {

    /*if (log.isDebugEnabled()) {
      log.debug("START_LOOP_RPC_SESSION at " + pollingFrequencyMs + " frequency");
    }*/

    // fetch value
    Callable<Optional<R>> loop =
        () -> {
          /*if (log.isDebugEnabled()) {
            log.debug("CYCLE_LOOP_RPC_SESSION at " + pollingFrequencyMs + " frequency");
          }*/
          return withSorobanClient(sorobanClient -> fetchValue.apply(sorobanClient));
        };

    // run loop until value found
    return asyncUtil.loopUntilSuccess(loop, pollingFrequencyMs, timeoutMs, () -> done);
  }

  public void exit() {
    done = true;
  }

  public boolean isDone() {
    return done;
  }
}
