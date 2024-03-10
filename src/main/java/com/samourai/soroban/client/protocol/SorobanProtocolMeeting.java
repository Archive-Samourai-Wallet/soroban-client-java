package com.samourai.soroban.client.protocol;

import com.samourai.wallet.bip47.BIP47UtilGeneric;
import com.samourai.wallet.bip47.rpc.BIP47Account;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.segwit.SegwitAddress;
import org.bitcoinj.core.NetworkParameters;

public class SorobanProtocolMeeting {
  public SorobanProtocolMeeting() {}

  public String getMeeetingAddressReceive(
      BIP47Account bip47Account,
      PaymentCode paymentCodeCounterparty,
      NetworkParameters params,
      BIP47UtilGeneric bip47Util)
      throws Exception {
    SegwitAddress receiveAddress =
        bip47Util.getReceiveAddress(bip47Account, paymentCodeCounterparty, 0, params);
    return receiveAddress.getBech32AsString();
  }

  public String getMeeetingAddressSend(
      BIP47Account bip47Account,
      PaymentCode paymentCodeInitiator,
      NetworkParameters params,
      BIP47UtilGeneric bip47Util)
      throws Exception {
    SegwitAddress sendAddress =
        bip47Util.getSendAddress(bip47Account, paymentCodeInitiator, 0, params);
    return sendAddress.getBech32AsString();
  }
}
