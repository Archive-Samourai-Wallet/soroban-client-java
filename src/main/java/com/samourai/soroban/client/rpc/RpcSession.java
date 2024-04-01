package com.samourai.soroban.client.rpc;

import com.samourai.soroban.client.SorobanClient;
import com.samourai.soroban.client.dialog.RpcDialog;
import com.samourai.soroban.client.exception.SorobanErrorMessageException;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import com.samourai.wallet.dexConfig.DexConfigProvider;
import com.samourai.wallet.dexConfig.SamouraiConfig;
import com.samourai.wallet.httpClient.HttpNetworkException;
import com.samourai.wallet.httpClient.HttpUsage;
import com.samourai.wallet.sorobanClient.RpcWallet;
import com.samourai.wallet.sorobanClient.SorobanServerDex;
import com.samourai.wallet.util.AsyncUtil;
import com.samourai.wallet.util.CallbackWithArg;
import com.samourai.wallet.util.RandomUtil;
import com.samourai.wallet.util.ShutdownException;
import com.samourai.wallet.util.urlStatus.UpStatusPool;
import io.reactivex.Single;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;
import org.apache.commons.lang3.RandomUtils;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcSession {
  private static final Logger log = LoggerFactory.getLogger(RpcSession.class.getName());
  private static final UpStatusPool upStatusPool = new SorobanUpStatusPool();
  private static final AsyncUtil asyncUtil = AsyncUtil.getInstance();

  private RpcClientService rpcClientService;
  private ECKey authenticationKey;
  private RpcWallet rpcWallet;
  private boolean done;
  private Collection<String> sorobanUrlsForced;
  private HttpUsage httpUsage;

  public RpcSession(RpcSession rpcSession) {
    this(rpcSession.rpcClientService, rpcSession.rpcWallet);
  }

  public RpcSession(RpcClientService rpcClientService, RpcWallet rpcWallet) {
    this.rpcClientService = rpcClientService;
    this.rpcWallet = rpcWallet;
    this.done = false;
    this.sorobanUrlsForced = null;
    this.httpUsage = HttpUsage.SOROBAN;
  }

  public void setAuthenticationKey(ECKey authenticationKey) {
    this.authenticationKey = authenticationKey;
  }

  public Collection<String> getSorobanUrlsUp() {
    boolean onion = rpcClientService.isOnion();
    return getSorobanUrlsUp(onion);
  }

  public Collection<String> getSorobanUrlsUp(boolean onion) {
    if (sorobanUrlsForced != null) {
      return sorobanUrlsForced;
    }
    NetworkParameters params = rpcClientService.getParams();
    Collection<String> sorobanUrls = SorobanServerDex.get(params).getSorobanUrls(onion);
    Collection<String> sorobanUrlsUp = upStatusPool.filterNotDown(sorobanUrls);
    if (sorobanUrlsUp.isEmpty()) {
      // retry when all down
      log.warn("All SorobanServerDex appears to be down, retrying all... onion=" + onion);
      upStatusPool.expireAll();
      return sorobanUrls;
    }
    return sorobanUrlsUp;
  }

  public <R> R withRpcSorobanUrl(CallbackWithArg<String, R> callable) throws Exception {
    int attempts = 0;

    // shuffle sorobanUrls
    List<String> sorobanUrls = new LinkedList<>(getSorobanUrlsUp());
    if (sorobanUrls.isEmpty()) {
      throw new HttpNetworkException("RPC failed: no SorobanServerDex available");
    }
    int nbServers = sorobanUrls.size();
    RandomUtil.getInstance().shuffle(sorobanUrls);
    Exception lastException = null;

    while (!sorobanUrls.isEmpty()) {
      // retry with each sorobanUrl
      String sorobanUrl = sorobanUrls.remove(0);
      try {
        R result = withRpcSorobanUrl(callable, sorobanUrl);
        if (attempts > 0) {
          attempts++;
          if (log.isDebugEnabled()) {
            log.debug(
                "RPC success: attempt "
                    + attempts
                    + "/"
                    + nbServers
                    + ", sorobanUrl="
                    + sorobanUrl);
          }
        }
        return result;
      } catch (HttpNetworkException e) {
        attempts++;
        lastException = e;
        log.warn(
            "RPC failed: attempt " + attempts + "/" + nbServers + ", sorobanUrl=" + sorobanUrl);
      } catch (ShutdownException e) {
        throw e; // abort on shutdown without log
      } catch (Throwable e) {
        if (log.isDebugEnabled()) {
          if (!(e instanceof TimeoutException) && !(e instanceof InterruptedException)) {
            if (e instanceof SorobanErrorMessageException) {
              log.warn("SorobanErrorMessage=" + e.getMessage() + ", sorobanUrl=" + sorobanUrl);
            } else {
              log.warn("error running withRpcSorobanUrl(), sorobanUrl=" + sorobanUrl, e);
            }
          }
        }
        throw e; // abort on non-http exception
      }
    }
    throw lastException;
  }

  public <R> R withRpcSorobanUrl(CallbackWithArg<String, R> callable, String sorobanUrlForced)
      throws Exception {
    if (sorobanUrlForced == null) {
      return withRpcSorobanUrl(callable);
    }
    try {
      R result = callable.apply(sorobanUrlForced + RpcClient.ENDPOINT_RPC);
      return result;
    } /*catch (TimeoutException e) {
        upStatusPool.setStatusDown(sorobanUrlForced, e.getMessage());
        throw e;
      } */ catch (Throwable e) {
      if (log.isDebugEnabled()) {
        if (!(e instanceof TimeoutException) && !(e instanceof InterruptedException)) {
          if (e instanceof SorobanErrorMessageException) {
            log.warn("SorobanErrorMessage=" + e.getMessage() + ", sorobanUrl=" + sorobanUrlForced);
          } else {
            log.warn("error running withRpcSorobanUrl(), sorobanUrl=" + sorobanUrlForced, e);
          }
        }
      }
      throw e;
    }
  }

  public <R> R withRpcClient(CallbackWithArg<RpcClient, R> callable) throws Exception {
    return withRpcClient(callable, null);
  }

  public <R> R withRpcClient(CallbackWithArg<RpcClient, R> callable, String sorobanUrlForced)
      throws Exception {
    return withRpcSorobanUrl(
        sorobanUrl -> {
          RpcClient rpcClient = rpcClientService.createRpcClient(sorobanUrl, httpUsage);
          if (authenticationKey != null) {
            rpcClient.setAuthenticationKey(authenticationKey);
          }
          return callable.apply(rpcClient);
        },
        sorobanUrlForced);
  }

  public <R> R withSorobanClient(
      CallbackWithArg<SorobanClient, R> callable, String sorobanUrlForced) throws Exception {
    Bip47Encrypter encrypter = rpcWallet.getBip47Encrypter();
    return withRpcClient(
        rpcClient -> {
          SorobanClient sorobanClient = new SorobanClient(rpcClient, encrypter);
          return callable.apply(sorobanClient);
        },
        sorobanUrlForced);
  }

  public <R extends Single> R withSorobanClientSingle(
      CallbackWithArg<SorobanClient, R> callable, String sorobanUrlForced) {
    try {
      return withSorobanClient(callable, sorobanUrlForced);
    } catch (Exception e) {
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

    int id = RandomUtils.nextInt();
    if (log.isTraceEnabled()) {
      log.trace("LOOP_RPC " + id + " START at " + pollingFrequencyMs + " frequency");
    }

    // fetch value
    Callable<Optional<R>> loop =
        () -> {
          if (log.isTraceEnabled()) {
            log.trace("LOOP_RPC " + id + " cycle");
          }
          Optional<R> result = withSorobanClient(sorobanClient -> fetchValue.apply(sorobanClient));
          if (result.isPresent()) {
            if (log.isTraceEnabled()) {
              log.trace("LOOP_RPC " + id + " SUCCESS -> result=" + result.get());
            }
          }
          return result;
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

  public void setSorobanUrlsForced(Collection<String> sorobanUrlsForced) {
    this.sorobanUrlsForced = sorobanUrlsForced;
  }

  public void setSorobanUrlsForcedV0(boolean testnet) {
    // use non-dex soroban
    Collection<String> sorobanUrlsV0 =
        computeSorobanUrlsForcedV0(testnet, rpcClientService.isOnion());
    setSorobanUrlsForced(sorobanUrlsV0);
  }

  private static Collection<String> computeSorobanUrlsForcedV0(boolean testnet, boolean onion) {
    SamouraiConfig samouraiConfig = DexConfigProvider.getInstance().getSamouraiConfig();
    String url;
    if (testnet) {
      url =
          onion
              ? samouraiConfig.getSorobanServerTestnetOnion()
              : samouraiConfig.getSorobanServerTestnetClear();
    } else {
      url =
          onion
              ? samouraiConfig.getSorobanServerMainnetOnion()
              : samouraiConfig.getSorobanServerMainnetClear();
    }
    return Arrays.asList(url);
  }

  public void setHttpUsage(HttpUsage httpUsage) {
    this.httpUsage = httpUsage;
  }
}
