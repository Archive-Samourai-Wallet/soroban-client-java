package com.samourai.soroban.client.endpoint.meta.wrapper;

import com.samourai.soroban.client.endpoint.meta.SorobanMetadata;
import com.samourai.soroban.client.exception.SorobanException;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.util.Pair;
import org.bitcoinj.core.ECKey;

/**
 * Sign {payload} with {sender}, verify signature against {sender}.<br>
 * Metadata: sender, signature
 */
public class SorobanWrapperMetaSignWithSender extends SorobanWrapperMetaSender {
  private SorobanWrapperMetaSign sorobanWrapperMetaSign;

  public SorobanWrapperMetaSignWithSender() {
    super();
    sorobanWrapperMetaSign = new SorobanWrapperMetaSign(null);
  }

  @Override
  public Pair<String, SorobanMetadata> onSend(
      Bip47Encrypter encrypter, Pair<String, SorobanMetadata> entry, Object initialPayload)
      throws Exception {
    // set sender
    super.onSend(encrypter, entry, initialPayload);

    // sign with sender
    ECKey signingKey = encrypter.getSigningKey();
    return sorobanWrapperMetaSign.sign(entry, signingKey);
  }

  @Override
  public Pair<String, SorobanMetadata> onReceive(
      Bip47Encrypter encrypter, Pair<String, SorobanMetadata> entry) throws Exception {
    // require sender
    super.onReceive(encrypter, entry);

    // find sender
    PaymentCode sender = SorobanWrapperMetaSender.getSender(entry.getRight());
    if (sender == null) {
      throw new SorobanException("Invalid payload: sender is null");
    }

    // verify signature against sender
    String signingAddress = encrypter.getSigningAddress(sender);
    return sorobanWrapperMetaSign.verifySignature(entry, signingAddress, encrypter.getParams());
  }
}
