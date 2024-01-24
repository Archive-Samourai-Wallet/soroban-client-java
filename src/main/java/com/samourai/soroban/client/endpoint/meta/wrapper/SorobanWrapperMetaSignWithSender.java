package com.samourai.soroban.client.endpoint.meta.wrapper;

import com.samourai.soroban.client.endpoint.meta.SorobanEntryMeta;
import com.samourai.soroban.client.exception.SorobanException;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.util.MessageSignUtilGeneric;
import org.bitcoinj.core.NetworkParameters;

/** Metadata: sender, signature */
public class SorobanWrapperMetaSignWithSender extends SorobanWrapperMetaSender {
  private static final MessageSignUtilGeneric messageSignUtil =
      MessageSignUtilGeneric.getInstance();
  private static final String META_SIGNATURE = "signature";

  @Override
  public SorobanEntryMeta onSend(
      Bip47Encrypter encrypter, SorobanEntryMeta sorobanEntry, Object initialPayload)
      throws Exception {
    // set sender
    sorobanEntry = super.onSend(encrypter, sorobanEntry, initialPayload);

    // add signature
    String payload = sorobanEntry.getPayload();
    String signature = encrypter.sign(payload);
    sorobanEntry.getMetadata().setMeta(META_SIGNATURE, signature);
    return sorobanEntry;
  }

  @Override
  public SorobanEntryMeta onReceive(Bip47Encrypter encrypter, SorobanEntryMeta sorobanEntry)
      throws Exception {
    // check sender
    sorobanEntry = super.onReceive(encrypter, sorobanEntry);

    // verify signature
    PaymentCode sender = getSender(sorobanEntry.getMetadata());
    NetworkParameters params = encrypter.getParams();
    String signingAddress = sender.notificationAddress(params).getAddressString();
    String payload = sorobanEntry.getPayload();
    String signature = sorobanEntry.getMetadata().getMetaString(META_SIGNATURE);
    if (!messageSignUtil.verifySignedMessage(signingAddress, payload, signature, params)) {
      throw new SorobanException("Invalid signature");
    }
    return sorobanEntry;
  }
}
