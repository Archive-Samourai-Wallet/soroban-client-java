package com.samourai.soroban.client.rpc;

import com.samourai.soroban.client.SorobanServerDex;
import com.samourai.soroban.client.dialog.Encrypter;
import com.samourai.soroban.client.dialog.RpcDialog;
import com.samourai.wallet.api.backend.beans.HttpException;
import com.samourai.wallet.util.CallbackWithArg;
import com.samourai.wallet.util.DownUrlService;
import com.samourai.wallet.util.RandomUtil;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcSession {
  private static final Logger log = LoggerFactory.getLogger(RpcSession.class.getName());
  private static final DownUrlService downUrlService = DownUrlService.getInstance();
  private static final int DOWN_DELAY_MS = 300000; // 5min

  private RpcClientService rpcClientService;
  private ECKey authenticationKey;

  public RpcSession(RpcClientService rpcClientService) {
    this.rpcClientService = rpcClientService;
  }

  public void setAuthenticationKey(ECKey authenticationKey) {
    this.authenticationKey = authenticationKey;
  }

  protected Collection<String> getServerUrlsUp() {
    NetworkParameters params = rpcClientService.getParams();
    boolean onion = rpcClientService.isOnion();
    Collection<String> serverUrls = SorobanServerDex.get(params).getServerUrls(onion);
    Collection<String> serverUrlsUp = downUrlService.filterUp(serverUrls);
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
        R result = callable.apply(serverUrl + RpcClient.ENDPOINT_RPC);
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
        downUrlService.setDown(serverUrl, DOWN_DELAY_MS);
        log.warn("RPC failed: attempt " + attempts + "/" + nbServers + ", url=" + serverUrl);
      } catch (Throwable e) {
        if (log.isDebugEnabled()) {
          log.error("RPC failed due to unmanaged exception, aborting", e);
        }
        throw e;
      }
    }
    throw lastException;
  }

  public <R> R withRpcClient(CallbackWithArg<RpcClient, R> callable) throws Exception {
    return withRpcServerUrl(
        serverUrl -> {
          RpcClient rpcClient = rpcClientService.createRpcClient(serverUrl);
          if (authenticationKey != null) {
            rpcClient.setAuthenticationKey(authenticationKey);
          }
          return callable.apply(rpcClient);
        });
  }

  public <R> R withRpcClientEncrypted(
      Encrypter encrypter, CallbackWithArg<RpcClientEncrypted, R> callable) throws Exception {
    return withRpcServerUrl(
        serverUrl -> {
          RpcClientEncrypted rpcClientEncrypted =
              rpcClientService.createRpcClientEncrypted(serverUrl, encrypter);
          if (authenticationKey != null) {
            rpcClientEncrypted.setAuthenticationKey(authenticationKey);
          }
          return callable.apply(rpcClientEncrypted);
        });
  }

  public RpcDialog createRpcDialog(Encrypter encrypter, String directory) throws Exception {
    return new RpcDialog(directory, this, encrypter);
  }

  public void close() {}
}
