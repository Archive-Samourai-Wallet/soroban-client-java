package com.samourai.soroban.client.endpoint;

import com.samourai.soroban.client.exception.SorobanException;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.util.Z85;

public class SorobanEncrypter {
  protected static final Z85 z85 = Z85.getInstance();

  private PaymentCode paymentCodePartner;

  public SorobanEncrypter(PaymentCode paymentCodePartner) {
    this.paymentCodePartner = paymentCodePartner;
  }

  public String encrypt(Bip47Encrypter encrypter, String payload) throws Exception {
    // encrypt payload
    return z85.encode(encrypter.encrypt(payload, paymentCodePartner));
  }

  public String decrypt(Bip47Encrypter encrypter, String payload) throws Exception {
    // decrypt payload
    try {
      return encrypter.decrypt(z85.decode(payload), paymentCodePartner);
    } catch (Exception e) {
      throw new SorobanException("Payload decryption failed");
    }
  }
}
