package com.samourai.soroban.client.wrapper;

import com.samourai.soroban.client.SorobanClient;
import com.samourai.soroban.client.SorobanPayload;
import com.samourai.soroban.client.exception.SorobanException;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.util.Z85;

public class SorobanWrapperEncrypt implements SorobanWrapper {
  protected static final Z85 z85 = Z85.getInstance();

  private PaymentCode paymentCodeReceiver;

  // sender
  public SorobanWrapperEncrypt(PaymentCode paymentCodeReceiver) {
    this.paymentCodeReceiver = paymentCodeReceiver;
  }

  // receiver
  public SorobanWrapperEncrypt() {
    this(null);
  }

  @Override
  public SorobanPayload onSend(SorobanClient sorobanClient, SorobanPayload sorobanPayload)
      throws Exception {
    if (paymentCodeReceiver == null) {
      throw new SorobanException("SorobanWrapperEncrypt.paymentCodeReceiver not configured");
    }

    // check sender
    Bip47Encrypter encrypter = sorobanClient.getEncrypter();
    String sender = encrypter.getPaymentCode().toString();
    if (!sender.equals(sorobanPayload.getSender())) {
      throw new SorobanException("sender mismatch");
    }

    // encrypt payload
    String payload = sorobanPayload.getPayload();
    String encryptedPayload = z85.encode(encrypter.encrypt(payload, paymentCodeReceiver));
    sorobanPayload.setPayload(encryptedPayload);
    return sorobanPayload;
  }

  @Override
  public SorobanPayload onReceive(SorobanClient sorobanClient, SorobanPayload sorobanPayload)
      throws Exception {
    // decrypt payload
    try {
      String sender = sorobanPayload.getSender();
      PaymentCode paymentCodeSender = new PaymentCode(sender);
      String payload = sorobanPayload.getPayload();
      Bip47Encrypter encrypter = sorobanClient.getEncrypter();
      String decryptedPayload = encrypter.decrypt(z85.decode(payload), paymentCodeSender);
      sorobanPayload.setPayload(decryptedPayload);
      return sorobanPayload;
    } catch (Exception e) {
      throw new SorobanException("Payload decryption failed");
    }
  }
}
