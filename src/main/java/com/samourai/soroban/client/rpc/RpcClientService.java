package com.samourai.soroban.client.rpc;

import com.samourai.http.client.HttpUsage;
import com.samourai.http.client.IHttpClient;
import com.samourai.http.client.IHttpClientService;
import com.samourai.soroban.client.dialog.RpcDialog;
import com.samourai.wallet.bip47.BIP47UtilGeneric;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.cahoots.CahootsWallet;
import com.samourai.wallet.crypto.CryptoUtil;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactoryGeneric;
import org.bitcoinj.core.NetworkParameters;

public class RpcClientService {
  private final IHttpClientService httpClientService;
  private final CryptoUtil cryptoUtil;
  private BIP47UtilGeneric bip47Util;
  private boolean onion;
  private NetworkParameters params;

  public RpcClientService(
      IHttpClientService httpClientService,
      CryptoUtil cryptoUtil,
      BIP47UtilGeneric bip47Util,
      boolean onion,
      NetworkParameters params) {
    this.httpClientService = httpClientService;
    this.cryptoUtil = cryptoUtil;
    this.bip47Util = bip47Util;
    this.onion = onion;
    this.params = params;
  }

  public RpcWalletImpl getRpcWallet(BIP47Wallet bip47Wallet) {
    return new RpcWalletImpl(bip47Wallet, cryptoUtil, bip47Util, this);
  }

  public RpcDialog createRpcDialog(CahootsWallet cahootsWallet, String directory)
      throws Exception { // TODO
    return getRpcWallet(cahootsWallet.getBip47Wallet())
        .createRpcSession()
        .createRpcDialog(directory);
  }

  public RpcWalletImpl generateRpcWallet() {
    HD_Wallet hdw = HD_WalletFactoryGeneric.getInstance().generateWallet(44, params);
    BIP47Wallet bip47Wallet = new BIP47Wallet(hdw);
    return new RpcWalletImpl(bip47Wallet, cryptoUtil, bip47Util, this);
  }

  protected RpcClient createRpcClient(String url) {
    IHttpClient httpClient = httpClientService.getHttpClient(HttpUsage.SOROBAN);
    return new RpcClient(httpClient, url, params);
  }

  public NetworkParameters getParams() {
    return params;
  }

  public boolean isOnion() {
    return onion;
  }
}
