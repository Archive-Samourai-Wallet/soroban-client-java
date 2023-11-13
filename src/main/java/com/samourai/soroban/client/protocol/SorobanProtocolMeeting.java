package com.samourai.soroban.client.protocol;

import com.samourai.soroban.client.RpcWallet;
import com.samourai.wallet.bip47.BIP47UtilGeneric;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.segwit.SegwitAddress;
import org.bitcoinj.core.NetworkParameters;

@Deprecated // TODO
public class SorobanProtocolMeeting {
  public SorobanProtocolMeeting() {}

  public String getMeeetingAddressReceive(
      RpcWallet rpcWallet,
      PaymentCode paymentCodeCounterparty,
      NetworkParameters params,
      BIP47UtilGeneric bip47Util)
      throws Exception {
    BIP47Wallet bip47Wallet = rpcWallet.getBip47Wallet();
    SegwitAddress receiveAddress =
        bip47Util.getReceiveAddress(bip47Wallet, paymentCodeCounterparty, 0, params);
    return receiveAddress.getBech32AsString();
  }

  public String getMeeetingAddressSend(
      RpcWallet rpcWallet,
      PaymentCode paymentCodeInitiator,
      NetworkParameters params,
      BIP47UtilGeneric bip47Util)
      throws Exception {
    BIP47Wallet bip47Wallet = rpcWallet.getBip47Wallet();
    SegwitAddress sendAddress =
        bip47Util.getSendAddress(bip47Wallet, paymentCodeInitiator, 0, params);
    return sendAddress.getBech32AsString();
  }
}
