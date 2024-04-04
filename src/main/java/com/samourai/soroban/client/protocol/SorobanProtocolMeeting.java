package com.samourai.soroban.client.protocol;

import com.samourai.soroban.client.SorobanConfig;
import com.samourai.wallet.bip47.BIP47UtilGeneric;
import com.samourai.wallet.bip47.rpc.BIP47Account;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.segwit.SegwitAddress;
import org.bitcoinj.core.NetworkParameters;

public class SorobanProtocolMeeting {
  private SorobanConfig sorobanConfig;

  public SorobanProtocolMeeting(SorobanConfig sorobanConfig) {
    this.sorobanConfig = sorobanConfig;
  }

  public String getMeeetingAddressReceive(
      BIP47Account bip47Account, PaymentCode paymentCodeCounterparty) throws Exception {
    BIP47UtilGeneric bip47Util = sorobanConfig.getExtLibJConfig().getBip47Util();
    NetworkParameters params = sorobanConfig.getExtLibJConfig().getSamouraiNetwork().getParams();
    SegwitAddress receiveAddress =
        bip47Util.getReceiveAddress(bip47Account, paymentCodeCounterparty, 0, params);
    return receiveAddress.getBech32AsString();
  }

  public String getMeeetingAddressSend(BIP47Account bip47Account, PaymentCode paymentCodeInitiator)
      throws Exception {
    BIP47UtilGeneric bip47Util = sorobanConfig.getExtLibJConfig().getBip47Util();
    NetworkParameters params = sorobanConfig.getExtLibJConfig().getSamouraiNetwork().getParams();
    SegwitAddress sendAddress =
        bip47Util.getSendAddress(bip47Account, paymentCodeInitiator, 0, params);
    return sendAddress.getBech32AsString();
  }
}
