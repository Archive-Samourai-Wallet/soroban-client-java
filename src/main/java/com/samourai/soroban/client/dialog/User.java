package com.samourai.soroban.client.dialog;

import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.bip47.rpc.java.Bip47UtilJava;
import com.samourai.wallet.segwit.SegwitAddress;
import org.bitcoinj.core.NetworkParameters;

public class User {
  private static final Bip47UtilJava bip47Util = Bip47UtilJava.getInstance();

  BIP47Wallet bip47Wallet;

  public User(BIP47Wallet bip47Wallet) {
    this.bip47Wallet = bip47Wallet;
  }

  public SegwitAddress getMeeetingAddressReceive(
      PaymentCode paymentCodeCounterparty, NetworkParameters params) throws Exception {
    SegwitAddress receiveAddress =
        bip47Util
            .getReceiveAddress(bip47Wallet, paymentCodeCounterparty, 0, params)
            .getSegwitAddressReceive();
    return receiveAddress;
  }

  public SegwitAddress getMeeetingAddressSend(
      PaymentCode paymentCodeInitiator, NetworkParameters params) throws Exception {
    SegwitAddress sendAddress =
        bip47Util
            .getSendAddress(bip47Wallet, paymentCodeInitiator, 0, params)
            .getSegwitAddressSend();
    return sendAddress;
  }

  public Box box() {
    return new Box();
  }

  // TODO ZL
  /*public String sharedSecret(Box box) {
    byte[] sharedSecret = box.sharedSecret();
    return Hex.toHexString(sharedSecret);
  }*/
}
