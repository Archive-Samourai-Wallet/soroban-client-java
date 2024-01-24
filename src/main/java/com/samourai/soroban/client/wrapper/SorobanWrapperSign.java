package com.samourai.soroban.client.wrapper;

import com.samourai.soroban.client.SorobanClient;
import com.samourai.soroban.client.SorobanPayload;
import com.samourai.soroban.client.exception.SorobanException;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.util.MessageSignUtilGeneric;
import org.bitcoinj.core.NetworkParameters;

public class SorobanWrapperSign implements SorobanWrapper {
  private static final MessageSignUtilGeneric messageSignUtil =
      MessageSignUtilGeneric.getInstance();
  private static final String META_SIGNATURE = "signature";

  @Override
  public SorobanPayload onSend(SorobanClient sorobanClient, SorobanPayload sorobanPayload)
      throws Exception {
    // check sender
    Bip47Encrypter encrypter = sorobanClient.getEncrypter();
    String sender = encrypter.getPaymentCode().toString();
    if (!sender.equals(sorobanPayload.getSender())) {
      throw new SorobanException("sender mismatch");
    }

    // add signature
    String payload = sorobanPayload.getPayload();
    String signature = encrypter.sign(payload);
    sorobanPayload.getMetadata().put(META_SIGNATURE, signature);
    return sorobanPayload;
  }

  @Override
  public SorobanPayload onReceive(SorobanClient sorobanClient, SorobanPayload sorobanPayload)
      throws Exception {
    // verify signature
    String sender = sorobanPayload.getSender();
    NetworkParameters params = sorobanClient.getParams();
    String signingAddress = new PaymentCode(sender).notificationAddress(params).getAddressString();
    String payload = sorobanPayload.getPayload();
    String signature = sorobanPayload.getMetadata().getString(META_SIGNATURE);
    if (!messageSignUtil.verifySignedMessage(signingAddress, payload, signature, params)) {
      throw new SorobanException("Invalid signature");
    }
    return sorobanPayload;
  }
}
