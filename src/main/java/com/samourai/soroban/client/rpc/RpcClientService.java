package com.samourai.soroban.client.rpc;

import com.samourai.soroban.client.SorobanConfig;
import com.samourai.wallet.bip47.rpc.BIP47Account;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactoryGeneric;
import com.samourai.wallet.httpClient.HttpUsage;
import com.samourai.wallet.httpClient.IHttpClient;
import org.bitcoinj.core.NetworkParameters;

public class RpcClientService {
  private final SorobanConfig sorobanConfig;
  private BIP47Account testModeBIP47Account; // for reproductible unit tests

  public RpcClientService(SorobanConfig sorobanConfig) {
    this.sorobanConfig = sorobanConfig;
    this.testModeBIP47Account = null;
  }

  public RpcWalletImpl getRpcWallet(BIP47Account bip47Account) {
    return new RpcWalletImpl(sorobanConfig, bip47Account);
  }

  public RpcWalletImpl generateRpcWallet() {
    BIP47Account bip47Account = generateBip47Account();
    return new RpcWalletImpl(sorobanConfig, bip47Account);
  }

  protected BIP47Account generateBip47Account() {
    if (testModeBIP47Account != null) {
      return testModeBIP47Account;
    }
    HD_Wallet hdw = HD_WalletFactoryGeneric.getInstance().generateWallet(44, getParams());
    return new BIP47Wallet(hdw).getAccount(0);
  }

  protected RpcClient createRpcClient(String url, HttpUsage httpUsage) {
    IHttpClient httpClient =
        sorobanConfig.getExtLibJConfig().getHttpClientService().getHttpClient(httpUsage);
    return new RpcClient(httpClient, url, getParams());
  }

  public NetworkParameters getParams() {
    return sorobanConfig.getExtLibJConfig().getSamouraiNetwork().getParams();
  }

  public boolean isOnion() {
    return sorobanConfig.getExtLibJConfig().isOnion();
  }

  public void _setTestModeBIP47Account(BIP47Account testModeBIP47Account) {
    this.testModeBIP47Account = testModeBIP47Account;
  }

  public BIP47Account _getTestModeBIP47Account() {
    return testModeBIP47Account;
  }
}
