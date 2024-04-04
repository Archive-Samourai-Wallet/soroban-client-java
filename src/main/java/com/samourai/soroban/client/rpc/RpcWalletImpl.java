package com.samourai.soroban.client.rpc;

import com.samourai.soroban.client.SorobanConfig;
import com.samourai.wallet.bip47.rpc.*;
import com.samourai.wallet.sorobanClient.RpcWallet;

public class RpcWalletImpl implements RpcWallet {
  private SorobanConfig sorobanConfig;
  private BIP47Account bip47Account;
  private Bip47Encrypter bip47Encrypter;

  public RpcWalletImpl(SorobanConfig sorobanConfig, BIP47Account bip47Account) {
    this.sorobanConfig = sorobanConfig;
    this.bip47Account = bip47Account;
    this.bip47Encrypter = new Bip47EncrypterImpl(sorobanConfig.getExtLibJConfig(), bip47Account);
  }

  @Override
  public BIP47Account getBip47Account() {
    return bip47Account;
  }

  @Override
  public Bip47Encrypter getBip47Encrypter() {
    return bip47Encrypter;
  }

  @Override
  public Bip47Partner getBip47Partner(PaymentCode paymentCodePartner, boolean initiator)
      throws Exception {
    return new Bip47PartnerImpl(
        sorobanConfig.getExtLibJConfig(), bip47Account, paymentCodePartner, initiator);
  }

  @Override
  public RpcWallet createNewIdentity() {
    return sorobanConfig.getRpcClientService().generateRpcWallet();
  }

  public RpcSession createRpcSession() {
    RpcClientService rpcClientService = sorobanConfig.getRpcClientService();
    return new RpcSession(rpcClientService, this);
  }
}
