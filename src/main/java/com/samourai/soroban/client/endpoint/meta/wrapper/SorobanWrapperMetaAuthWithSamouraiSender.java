package com.samourai.soroban.client.endpoint.meta.wrapper;

import com.samourai.soroban.client.endpoint.meta.SorobanMetadata;
import com.samourai.soroban.client.exception.SorobanException;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.util.MessageSignUtilGeneric;
import com.samourai.wallet.util.Pair;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolNetwork;

/** Metadata: auth */
public class SorobanWrapperMetaAuthWithSamouraiSender extends AbstractSorobanWrapperMetaAuth {
  private static final MessageSignUtilGeneric messageSignUtil =
      MessageSignUtilGeneric.getInstance();

  private WhirlpoolNetwork whirlpoolNetwork;

  // for coordinator
  public SorobanWrapperMetaAuthWithSamouraiSender(
      WhirlpoolNetwork whirlpoolNetwork, String senderSignedBySigningAddress) {
    super(senderSignedBySigningAddress);
    this.whirlpoolNetwork = whirlpoolNetwork;
  }

  // for client
  public SorobanWrapperMetaAuthWithSamouraiSender(WhirlpoolNetwork whirlpoolNetwork) {
    this(whirlpoolNetwork, null);
  }

  @Override
  protected void validateAuth(
      Bip47Encrypter encrypter, Pair<String, SorobanMetadata> entry, String auth)
      throws SorobanException {
    // require sender
    PaymentCode sender = SorobanWrapperMetaSender.getSender(entry.getRight());
    if (sender == null) {
      throw new SorobanException("Missing metadata.sender to validate metadata.auth");
    }

    // validate auth as sender signed by signingAddress
    String samouraiAddress = whirlpoolNetwork.getSigningAddress();
    if (!messageSignUtil.verifySignedMessage(
        samouraiAddress, sender.toString(), auth, whirlpoolNetwork.getParams())) {
      throw new SorobanException("Invalid metadata.auth against metadata.sender");
    }
  }
}
