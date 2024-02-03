package com.samourai.soroban.client.endpoint.meta.wrapper;

import com.samourai.soroban.client.endpoint.meta.SorobanMetadata;
import com.samourai.soroban.client.exception.SorobanException;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.util.MessageSignUtilGeneric;
import com.samourai.wallet.util.Pair;
import org.bitcoinj.core.NetworkParameters;

/** Metadata: sender, signature */
public class SorobanWrapperMetaSignWithSender extends SorobanWrapperMetaSender {
  private static final MessageSignUtilGeneric messageSignUtil =
      MessageSignUtilGeneric.getInstance();
  private static final String META_SIGNATURE = "signature";

  @Override
  public Pair<String, SorobanMetadata> onSend(
      Bip47Encrypter encrypter, Pair<String, SorobanMetadata> entry, Object initialPayload)
      throws Exception {
    // set sender
    entry = super.onSend(encrypter, entry, initialPayload);

    // add signature
    String payload = entry.getLeft();
    String signature = encrypter.sign(payload);
    entry.getRight().setMeta(META_SIGNATURE, signature);
    return entry;
  }

  @Override
  public Pair<String, SorobanMetadata> onReceive(
      Bip47Encrypter encrypter, Pair<String, SorobanMetadata> entry) throws Exception {
    // check sender
    entry = super.onReceive(encrypter, entry);

    // verify signature
    PaymentCode sender = getSender(entry.getRight());
    NetworkParameters params = encrypter.getParams();
    String signingAddress = sender.notificationAddress(params).getAddressString();
    String payload = entry.getLeft();
    String signature = entry.getRight().getMetaString(META_SIGNATURE);
    if (!messageSignUtil.verifySignedMessage(signingAddress, payload, signature, params)) {
      throw new SorobanException("Invalid signature");
    }
    return entry;
  }
}
