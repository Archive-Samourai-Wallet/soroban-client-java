package com.samourai.soroban.client.endpoint.meta.wrapper;

import com.samourai.soroban.client.endpoint.meta.SorobanMetadata;
import com.samourai.soroban.client.exception.SorobanException;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import com.samourai.wallet.util.MessageSignUtilGeneric;
import com.samourai.wallet.util.Pair;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;

/**
 * Sign {payload} with a given signing key, verify signature with a given signing address.<br>
 * Metadata: signature
 */
public class SorobanWrapperMetaSign implements SorobanWrapperMeta {
  private static final MessageSignUtilGeneric messageSignUtil =
      MessageSignUtilGeneric.getInstance();
  private static final String META_SIGNATURE = "signature";

  private ECKey signingKey;
  private String signingAddress;

  // for sender/receiver
  public SorobanWrapperMetaSign(ECKey signingKey, NetworkParameters params) {
    this.signingKey = signingKey;
    this.signingAddress = signingKey.toAddress(params).toString();
  }

  // for receiver
  public SorobanWrapperMetaSign(String signingAddress) {
    this.signingKey = null;
    this.signingAddress = signingAddress;
  }

  @Override
  public Pair<String, SorobanMetadata> onSend(
      Bip47Encrypter encrypter, Pair<String, SorobanMetadata> entry, Object initialPayload)
      throws Exception {
    return sign(entry, signingKey);
  }

  protected Pair<String, SorobanMetadata> sign(
      Pair<String, SorobanMetadata> entry, ECKey signingKey) throws SorobanException {
    if (signingKey == null) {
      throw new SorobanException("SorobanWrapperSign failed: signingKey not configured");
    }
    // add signature
    String payload = entry.getLeft();
    String signature = messageSignUtil.signMessage(signingKey, payload);
    entry.getRight().setMeta(META_SIGNATURE, signature);
    return entry;
  }

  @Override
  public Pair<String, SorobanMetadata> onReceive(
      Bip47Encrypter encrypter, Pair<String, SorobanMetadata> entry) throws Exception {
    NetworkParameters params = encrypter.getParams();
    return verifySignature(entry, signingAddress, params);
  }

  protected Pair<String, SorobanMetadata> verifySignature(
      Pair<String, SorobanMetadata> entry, String signingAddress, NetworkParameters params)
      throws SorobanException {
    if (signingAddress == null) {
      throw new SorobanException("SorobanWrapperSign failed: signingAddress not configured");
    }
    // verify signature
    String payload = entry.getLeft();
    String signature = entry.getRight().getMetaString(META_SIGNATURE);
    if (!messageSignUtil.verifySignedMessage(signingAddress, payload, signature, params)) {
      throw new SorobanException("Invalid signature");
    }
    return entry;
  }
}
