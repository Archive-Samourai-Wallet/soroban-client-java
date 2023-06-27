package com.samourai.soroban.client.rpc;

import com.samourai.http.client.HttpUsage;
import com.samourai.http.client.IHttpClient;
import com.samourai.http.client.IHttpClientService;
import com.samourai.soroban.client.dialog.Encrypter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bitcoinj.core.NetworkParameters;

public class RpcClientService {
  private final IHttpClientService httpClientService;
  private boolean onion;
  private NetworkParameters params;
  private Map<String, RpcSession> rpcSessions;

  public RpcClientService(
      IHttpClientService httpClientService, boolean onion, NetworkParameters params) {
    this.httpClientService = httpClientService;
    this.onion = onion;
    this.params = params;
    this.rpcSessions = new LinkedHashMap<>();
  }

  public RpcSession getRpcSession(String key) {
    if (!rpcSessions.containsKey(key)) {
      rpcSessions.put(key, createRpcSession());
    }
    return rpcSessions.get(key);
  }

  public RpcSession createRpcSession() {
    return new RpcSession(this);
  }

  /*public RpcClient createRpcClientRandomServer() {
    return createRpcClientRandomServer(null);
  }

  public RpcClient createRpcClientRandomServer(ECKey authenticationKey) {
    // use random SorobanServerDex
    String url = SorobanServerDex.get(params).getServerUrlRandom(onion) + RpcClient.ENDPOINT_RPC;
    return createRpcClient(authenticationKey, url);
  }*/

  protected RpcClient createRpcClient(String url) {
    IHttpClient httpClient = httpClientService.getHttpClient(HttpUsage.SOROBAN);
    return new RpcClient(httpClient, url, params);
  }

  protected RpcClientEncrypted createRpcClientEncrypted(String url, Encrypter encrypter) {
    IHttpClient httpClient = httpClientService.getHttpClient(HttpUsage.SOROBAN);
    return new RpcClientEncrypted(httpClient, url, params, encrypter);
  }

  public NetworkParameters getParams() {
    return params;
  }

  public boolean isOnion() {
    return onion;
  }
}
