package com.samourai.soroban.client.rpc;

import com.samourai.wallet.bip47.BIP47UtilGeneric;
import com.samourai.wallet.bip47.rpc.BIP47Account;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.crypto.CryptoUtil;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactoryGeneric;
import com.samourai.wallet.httpClient.HttpUsage;
import com.samourai.wallet.httpClient.IHttpClient;
import com.samourai.wallet.httpClient.IHttpClientService;
import com.samourai.wallet.util.ExtLibJConfig;
import org.bitcoinj.core.NetworkParameters;

public class RpcClientService {
  private final IHttpClientService httpClientService;
  private final CryptoUtil cryptoUtil;
  private BIP47UtilGeneric bip47Util;
  private boolean onion;
  private NetworkParameters params;
  private BIP47Account testModeBIP47Account; // for reproductible unit tests

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
    this.testModeBIP47Account = null;
  }

  public RpcClientService(ExtLibJConfig extLibJConfig) {
    this(
        extLibJConfig.getHttpClientService(),
        extLibJConfig.getCryptoUtil(),
        extLibJConfig.getBip47Util(),
        extLibJConfig.isOnion(),
        extLibJConfig.getSamouraiNetwork().getParams());
  }

  public RpcWalletImpl getRpcWallet(BIP47Account bip47Account) {
    return new RpcWalletImpl(bip47Account, cryptoUtil, bip47Util, this);
  }

  public RpcWalletImpl generateRpcWallet() {
    BIP47Account bip47Account = generateBip47Account();
    return new RpcWalletImpl(bip47Account, cryptoUtil, bip47Util, this);
  }

  protected BIP47Account generateBip47Account() {
    if (testModeBIP47Account != null) {
      return testModeBIP47Account;
    }
    HD_Wallet hdw = HD_WalletFactoryGeneric.getInstance().generateWallet(44, params);
    return new BIP47Wallet(hdw).getAccount(0);
  }

  protected RpcClient createRpcClient(String url, HttpUsage httpUsage) {
    IHttpClient httpClient = httpClientService.getHttpClient(httpUsage);
    return new RpcClient(httpClient, url, params);
  }

  public NetworkParameters getParams() {
    return params;
  }

  public boolean isOnion() {
    return onion;
  }

  public void _setTestModeBIP47Account(BIP47Account testModeBIP47Account) {
    this.testModeBIP47Account = testModeBIP47Account;
  }

  public BIP47Account _getTestModeBIP47Account() {
    return testModeBIP47Account;
  }
}
