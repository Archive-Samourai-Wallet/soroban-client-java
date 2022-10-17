package com.samourai.soroban.client.rpc;

import com.samourai.http.client.IHttpClient;
import com.samourai.soroban.client.SorobanMessage;
import com.samourai.soroban.client.SorobanServer;
import com.samourai.soroban.client.dialog.Encrypter;
import com.samourai.soroban.client.dialog.PaynymEncrypter;
import com.samourai.soroban.client.dialog.RpcDialog;
import com.samourai.wallet.cahoots.CahootsWallet;
import com.samourai.wallet.crypto.CryptoUtil;
import org.bitcoinj.core.NetworkParameters;

public class RpcService {
  protected static final String ENDPOINT_RPC = "/rpc";

  private final IHttpClient httpClient;
  private final CryptoUtil cryptoUtil;
  private boolean onion;

  public RpcService(IHttpClient httpClient, CryptoUtil cryptoUtil, boolean onion) {
    this.httpClient = httpClient;
    this.cryptoUtil = cryptoUtil;
    this.onion = onion;
  }

  public RpcClient createRpcClient(CahootsWallet cahootsWallet) {
    return createRpcClient(cahootsWallet.getParams());
  }

  public RpcClient createRpcClient(NetworkParameters params) {
    return new RpcClient(httpClient, SorobanServer.get(params).getServerUrl(onion) + ENDPOINT_RPC);
  }

  public RpcDialog createRpcDialog(CahootsWallet cahootsWallet, String info, String directory)
      throws Exception {
    RpcClient rpcClient = createRpcClient(cahootsWallet);
    Encrypter encrypter = getEncrypter(cahootsWallet);
    return new RpcDialog(rpcClient, encrypter, info, directory);
  }

  public RpcDialog createRpcDialog(
      CahootsWallet cahootsWallet, String info, SorobanMessage sorobanMessage) throws Exception {
    return createRpcDialog(cahootsWallet, info, sorobanMessage.toPayload());
  }

  private Encrypter getEncrypter(CahootsWallet cahootsWallet) {
    return new PaynymEncrypter(
        cahootsWallet.getBip47Account().getNotificationAddress().getECKey(),
        cahootsWallet.getParams(),
        cryptoUtil);
  }
}
