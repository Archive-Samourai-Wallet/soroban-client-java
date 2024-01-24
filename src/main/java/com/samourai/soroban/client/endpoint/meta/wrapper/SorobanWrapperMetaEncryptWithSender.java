package com.samourai.soroban.client.endpoint.meta.wrapper;

import com.samourai.soroban.client.endpoint.meta.SorobanEntryMeta;
import com.samourai.soroban.client.endpoint.wrapper.SorobanWrapperEncrypt;
import com.samourai.soroban.client.exception.SorobanException;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import com.samourai.wallet.bip47.rpc.PaymentCode;

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
  public SorobanEntryMeta onSend(
      Bip47Encrypter encrypter, SorobanEntryMeta sorobanEntry, Object initialPayload)
      throws Exception {
    if (paymentCodeReceiver == null) {
      throw new SorobanException("SorobanWrapperEncrypt.paymentCodeReceiver not configured");
    }

    // set sender
    sorobanEntry = super.onSend(encrypter, sorobanEntry, initialPayload);

    // encrypt payload
    String payload = sorobanEntry.getPayload();
    String encryptedPayload =
        new SorobanWrapperEncrypt(paymentCodeReceiver).onSend(encrypter, payload, initialPayload);
    sorobanEntry.setPayload(encryptedPayload);
    return sorobanEntry;
  }

  @Override
  public SorobanEntryMeta onReceive(Bip47Encrypter encrypter, SorobanEntryMeta sorobanEntry)
      throws Exception {
    // check sender
    sorobanEntry = super.onReceive(encrypter, sorobanEntry);

    // decrypt payload
    try {
      PaymentCode sender = getSender(sorobanEntry.getMetadata());
      String encryptedPayload = sorobanEntry.getPayload();
      String decryptedPayload =
          new SorobanWrapperEncrypt(sender).onReceive(encrypter, encryptedPayload);
      sorobanEntry.setPayload(decryptedPayload);
    } catch (Exception e) {
      throw new SorobanException("Payload decryption failed");
    }
    return sorobanEntry;
  }
}
