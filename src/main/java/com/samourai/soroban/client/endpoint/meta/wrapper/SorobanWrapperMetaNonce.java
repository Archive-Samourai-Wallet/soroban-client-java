package com.samourai.soroban.client.endpoint.meta.wrapper;

import com.samourai.soroban.client.endpoint.meta.SorobanEntryMeta;
import com.samourai.soroban.client.endpoint.meta.SorobanItem;
import com.samourai.soroban.client.endpoint.meta.SorobanMetadata;
import com.samourai.soroban.client.exception.SorobanException;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
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
  public SorobanEntryMeta onSend(
      Bip47Encrypter encrypter, SorobanEntryMeta sorobanEntry, Object initialPayload)
      throws Exception {
    // set nonce
    sorobanEntry.getMetadata().setMeta(KEY_NONCE, System.currentTimeMillis());
    return sorobanEntry;
  }

  @Override
  public SorobanEntryMeta onReceive(Bip47Encrypter encrypter, SorobanEntryMeta sorobanEntry)
      throws Exception {
    // require nonce
    Long nonce = getNonce(sorobanEntry.getMetadata());
    if (nonce == null) {
      throw new SorobanException("Invalid metadata.nonce: " + sorobanEntry.getMetadata());
    }
    return sorobanEntry;
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
