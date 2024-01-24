package com.samourai.soroban.client.endpoint.meta.wrapper;

import com.samourai.soroban.client.endpoint.meta.SorobanEntryMeta;
import com.samourai.soroban.client.endpoint.meta.SorobanItem;
import com.samourai.soroban.client.endpoint.meta.SorobanMetadata;
import com.samourai.soroban.client.exception.SorobanException;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import java.util.function.Predicate;
import org.apache.commons.lang3.StringUtils;

/** Metadata: type */
public class SorobanWrapperMetaType implements SorobanWrapperMeta {
  private static final String KEY_TYPE = "type";

  @Override
  public SorobanEntryMeta onSend(
      Bip47Encrypter encrypter, SorobanEntryMeta sorobanEntry, Object initialPayload)
      throws Exception {
    // set type
    String type = initialPayload.getClass().getName();
    sorobanEntry.getMetadata().setMeta(KEY_TYPE, type);
    return sorobanEntry;
  }

  @Override
  public SorobanEntryMeta onReceive(Bip47Encrypter encrypter, SorobanEntryMeta sorobanEntry)
      throws Exception {
    // require type
    String type = getType(sorobanEntry.getMetadata());
    if (StringUtils.isEmpty(type)) {
      throw new SorobanException("Missing metadata.type: " + sorobanEntry.getMetadata());
    }
    return sorobanEntry;
  }

  public static String getType(SorobanMetadata metadata) {
    return metadata.getMetaString(KEY_TYPE);
  }

  public static <I extends SorobanItem> Predicate<I> filterByType(Class type) {
    return item -> getType(item.getMetadata()).equals(type.getName());
  }
}
