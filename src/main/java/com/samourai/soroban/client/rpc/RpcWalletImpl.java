package com.samourai.soroban.client.rpc;

import com.samourai.wallet.bip47.BIP47UtilGeneric;
import com.samourai.wallet.bip47.rpc.*;
import com.samourai.wallet.crypto.CryptoUtil;
import com.samourai.wallet.sorobanClient.RpcWallet;

public class RpcWalletImpl implements RpcWallet {
  private BIP47Account bip47Account;
  private CryptoUtil cryptoUtil;
  private BIP47UtilGeneric bip47Util;
  private Bip47Encrypter bip47Encrypter;
  private RpcClientService rpcClientService; // TODO

  public RpcWalletImpl(
      BIP47Account bip47Account,
      CryptoUtil cryptoUtil,
      BIP47UtilGeneric bip47Util,
      RpcClientService rpcClientService) {
    this.bip47Account = bip47Account;
    this.cryptoUtil = cryptoUtil;
    this.bip47Util = bip47Util;
    this.bip47Encrypter = new Bip47EncrypterImpl(bip47Account, cryptoUtil, bip47Util);
    this.rpcClientService = rpcClientService;
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
    return new Bip47PartnerImpl(bip47Account, paymentCodePartner, initiator, cryptoUtil, bip47Util);
  }

  @Override
  public RpcWallet createNewIdentity() {
    return rpcClientService.generateRpcWallet();
  }

  public RpcSession createRpcSession() {
    return new RpcSession(rpcClientService, this);
  }
}
