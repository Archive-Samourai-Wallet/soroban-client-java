package com.samourai.soroban.client.rpc;

import com.samourai.http.client.IHttpClient;
import com.samourai.wallet.crypto.CryptoUtil;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;

public class RpcClientService {
  private final IHttpClient httpClient;
  private final CryptoUtil cryptoUtil;
  private boolean onion;
  private NetworkParameters params;
  private Map<String, RpcClient> rpcClients;

  public RpcClientService(
      IHttpClient httpClient, CryptoUtil cryptoUtil, boolean onion, NetworkParameters params) {
    this.httpClient = httpClient;
    this.cryptoUtil = cryptoUtil;
    this.onion = onion;
    this.params = params;
    this.rpcClients = new LinkedHashMap<>();
  }

  public RpcClient getRpcClient(String info) {
    return getRpcClient(info, null);
  }

  public RpcClient getRpcClient(String info, ECKey authenticationKey) {
    String k = info + (authenticationKey != null ? authenticationKey.getPublicKeyAsHex() : "");
    if (!rpcClients.containsKey(k)) {
      rpcClients.put(k, createRpcClient(info, authenticationKey));
    }
    return rpcClients.get(k);
  }

  protected RpcClient createRpcClient(String info, ECKey authenticationKey) {
    RpcClient rpcClient = new RpcClient(info, httpClient, cryptoUtil, params, onion);
    if (authenticationKey != null) {
      rpcClient.setAuthentication(authenticationKey, params);
    }
    return rpcClient;
  }
}
