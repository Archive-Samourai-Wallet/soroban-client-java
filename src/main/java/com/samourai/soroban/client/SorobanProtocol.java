package com.samourai.soroban.client;

import com.samourai.wallet.bip47.BIP47UtilGeneric;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.segwit.SegwitAddress;
import org.bitcoinj.core.NetworkParameters;

public class SorobanProtocol {
  public SorobanProtocol() {}

  public String getMeeetingAddressReceive(
      RpcWallet rpcWallet,
      PaymentCode paymentCodeCounterparty,
      NetworkParameters params,
      BIP47UtilGeneric bip47Util)
      throws Exception {
    SegwitAddress receiveAddress =
        bip47Util.getReceiveAddress(rpcWallet, paymentCodeCounterparty, 0, params);
    return receiveAddress.getBech32AsString();
  }

  public String getMeeetingAddressSend(
      RpcWallet rpcWallet,
      PaymentCode paymentCodeInitiator,
      NetworkParameters params,
      BIP47UtilGeneric bip47Util)
      throws Exception {
    SegwitAddress sendAddress =
        bip47Util.getSendAddress(rpcWallet, paymentCodeInitiator, 0, params);
    return sendAddress.getBech32AsString();
  }
}
