package com.samourai.soroban.client.dialog;

import com.samourai.wallet.bip47.rpc.PaymentCode;

public interface Encrypter {
  String decrypt(byte[] payload, PaymentCode paymentCodePartner) throws Exception;

  byte[] encrypt(String payload, PaymentCode paymentCodePartner) throws Exception;

  PaymentCode getPaymentCode();
}
