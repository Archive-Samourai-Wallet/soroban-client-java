package com.samourai.soroban.client.rpc;

import com.samourai.http.client.IHttpClient;
import com.samourai.soroban.client.SorobanMessage;
import com.samourai.soroban.client.SorobanServer;
import com.samourai.soroban.client.dialog.Encrypter;
import com.samourai.soroban.client.dialog.PaynymEncrypter;
import com.samourai.soroban.client.dialog.RpcDialog;
import com.samourai.wallet.cahoots.CahootsWallet;
import java.security.Provider;
import org.bitcoinj.core.NetworkParameters;

public class RpcService {
  private static final String ENDPOINT_RPC = "/rpc";

  private final IHttpClient httpClient;
  private final Provider provider;
  private boolean onion;

  public RpcService(IHttpClient httpClient, Provider provider, boolean onion) {
    this.httpClient = httpClient;
    this.provider = provider;
    this.onion = onion;
  }

  public RpcClient createRpcClient(CahootsWallet cahootsWallet) {
    return createRpcClient(onion, cahootsWallet.getParams());
  }

  public RpcClient createRpcClient(boolean onion, NetworkParameters params) {
    return new RpcClient(httpClient, SorobanServer.get(params).getServerUrl(onion) + ENDPOINT_RPC);
  }

  public RpcDialog createRpcDialog(CahootsWallet cahootsWallet, String directory) throws Exception {
    RpcClient rpcClient = createRpcClient(cahootsWallet);
    Encrypter encrypter = getEncrypter(cahootsWallet);
    return new RpcDialog(rpcClient, encrypter, directory);
  }

  public RpcDialog createRpcDialog(CahootsWallet cahootsWallet, SorobanMessage sorobanMessage)
      throws Exception {
    return createRpcDialog(cahootsWallet, sorobanMessage.toPayload());
  }

  private Encrypter getEncrypter(CahootsWallet cahootsWallet) {
    return new PaynymEncrypter(
        cahootsWallet.getBip47Account().getNotificationAddress().getECKey(),
        cahootsWallet.getParams(),
        provider);
  }
}
