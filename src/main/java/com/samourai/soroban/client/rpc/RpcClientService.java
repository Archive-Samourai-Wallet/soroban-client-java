package com.samourai.soroban.client.rpc;

import com.samourai.http.client.IHttpClient;
import com.samourai.wallet.crypto.CryptoUtil;
import java.util.LinkedHashMap;
import java.util.Map;
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
    if (!rpcClients.containsKey(info)) {
      rpcClients.put(info, createRpcClient(info));
    }
    return rpcClients.get(info);
  }

  private RpcClient createRpcClient(String info) {
    return new RpcClient(info, httpClient, cryptoUtil, params, onion);
  }
}
