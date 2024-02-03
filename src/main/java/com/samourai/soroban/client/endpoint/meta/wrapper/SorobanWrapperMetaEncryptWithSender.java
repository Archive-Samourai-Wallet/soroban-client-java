package com.samourai.soroban.client.endpoint.meta.wrapper;

import com.samourai.soroban.client.endpoint.meta.SorobanMetadata;
import com.samourai.soroban.client.endpoint.wrapper.SorobanWrapperEncrypt;
import com.samourai.soroban.client.exception.SorobanException;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.util.Pair;

/** Metadata: sender */
public class SorobanWrapperMetaEncryptWithSender extends SorobanWrapperMetaSender {
  private PaymentCode paymentCodeReceiver;

  // sender
  public SorobanWrapperMetaEncryptWithSender(PaymentCode paymentCodeReceiver) {
    this.paymentCodeReceiver = paymentCodeReceiver;
  }

  // receiver
  public SorobanWrapperMetaEncryptWithSender() {
    this(null);
  }

  @Override
  public Pair<String, SorobanMetadata> onSend(
      Bip47Encrypter encrypter, Pair<String, SorobanMetadata> entry, Object initialPayload)
      throws Exception {
    if (paymentCodeReceiver == null) {
      throw new SorobanException("SorobanWrapperEncrypt.paymentCodeReceiver not configured");
    }

    // set sender
    entry = super.onSend(encrypter, entry, initialPayload);

    // encrypt payload
    String payload = entry.getLeft();
    String encryptedPayload =
        new SorobanWrapperEncrypt(paymentCodeReceiver).onSend(encrypter, payload, initialPayload);
    return Pair.of(encryptedPayload, entry.getRight());
  }

  @Override
  public Pair<String, SorobanMetadata> onReceive(
      Bip47Encrypter encrypter, Pair<String, SorobanMetadata> entry) throws Exception {
    // check sender
    entry = super.onReceive(encrypter, entry);

    // decrypt payload
    try {
      PaymentCode sender = getSender(entry.getRight());
      String encryptedPayload = entry.getLeft();
      String decryptedPayload =
          new SorobanWrapperEncrypt(sender).onReceive(encrypter, encryptedPayload);
      return Pair.of(decryptedPayload, entry.getRight());
    } catch (Exception e) {
      throw new SorobanException("Payload decryption failed");
    }
  }
}
