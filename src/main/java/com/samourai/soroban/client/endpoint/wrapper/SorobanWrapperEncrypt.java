package com.samourai.soroban.client.endpoint.wrapper;

import com.samourai.soroban.client.exception.SorobanException;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.util.Z85;

public class SorobanWrapperEncrypt implements SorobanWrapperString {
  protected static final Z85 z85 = Z85.getInstance();

  private PaymentCode paymentCodePartner;

  public SorobanWrapperEncrypt(PaymentCode paymentCodePartner) {
    this.paymentCodePartner = paymentCodePartner;
  }

  @Override
  public String onSend(Bip47Encrypter encrypter, String payload, Object initialPayload)
      throws Exception {
    // encrypt payload
    return z85.encode(encrypter.encrypt(payload, paymentCodePartner));
  }

  @Override
  public String onReceive(Bip47Encrypter encrypter, String payload) throws Exception {
    // decrypt payload
    try {
      return encrypter.decrypt(z85.decode(payload), paymentCodePartner);
    } catch (Exception e) {
      throw new SorobanException("Payload decryption failed");
    }
  }

  // for tests
  public void setPaymentCodePartner(PaymentCode paymentCodePartner) {
    this.paymentCodePartner = paymentCodePartner;
  }
}
