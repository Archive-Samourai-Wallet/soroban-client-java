package com.samourai.soroban.client.endpoint.meta.wrapper;

import com.samourai.soroban.client.endpoint.meta.SorobanItem;
import com.samourai.soroban.client.endpoint.meta.SorobanMetadata;
import com.samourai.soroban.client.exception.SorobanException;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import com.samourai.wallet.util.Pair;
import java.util.Comparator;
import java.util.function.BinaryOperator;

/** Metadata: sender */
public class SorobanWrapperMetaNonce implements SorobanWrapperMeta {
  private static final String KEY_NONCE = "nonce";
  private static final Comparator<SorobanItem> comparatorByNonce =
      Comparator.comparing(o -> getNonce(o.getMetadata()));
  private static final BinaryOperator<SorobanItem> mergeLastByNonce =
      (a, b) -> comparatorByNonce.compare(a, b) > 0 ? a : b;

  @Override
  public Pair<String, SorobanMetadata> onSend(
      Bip47Encrypter encrypter, Pair<String, SorobanMetadata> entry, Object initialPayload)
      throws Exception {
    // set nonce
    entry.getRight().setMeta(KEY_NONCE, System.currentTimeMillis());
    return entry;
  }

  @Override
  public Pair<String, SorobanMetadata> onReceive(
      Bip47Encrypter encrypter, Pair<String, SorobanMetadata> entry) throws Exception {
    // require nonce
    Long nonce = getNonce(entry.getRight());
    if (nonce == null) {
      throw new SorobanException("Invalid metadata.nonce: " + entry.getRight());
    }
    return entry;
  }

  public static Long getNonce(SorobanMetadata metadata) {
    Long nonce = metadata.getMetaLong(KEY_NONCE);
    if (nonce == null || nonce <= 0) {
      return null;
    }
    return nonce;
  }

  public static Comparator<SorobanItem> getComparatorByNonce() {
    return comparatorByNonce;
  }

  public static BinaryOperator<SorobanItem> mergeLastByNonce() {
    return mergeLastByNonce;
  }
}
