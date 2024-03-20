package com.samourai.soroban.client.endpoint.meta.wrapper;

import com.samourai.soroban.client.endpoint.meta.SorobanItem;
import com.samourai.soroban.client.endpoint.meta.SorobanMetadata;
import com.samourai.soroban.client.exception.SorobanException;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import com.samourai.wallet.util.Pair;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/** Metadata: type */
public class SorobanWrapperMetaType implements SorobanWrapperMeta {
  private static final String KEY_TYPE = "type";

  @Override
  public Pair<String, SorobanMetadata> onSend(
      Bip47Encrypter encrypter, Pair<String, SorobanMetadata> entry, Object initialPayload)
      throws Exception {
    // set type
    String type = initialPayload.getClass().getName();
    entry.getRight().setMeta(KEY_TYPE, type);
    return entry;
  }

  @Override
  public Pair<String, SorobanMetadata> onReceive(
      Bip47Encrypter encrypter, Pair<String, SorobanMetadata> entry) throws Exception {
    // require type
    String type = getType(entry.getRight());
    if (StringUtils.isEmpty(type)) {
      throw new SorobanException("Missing metadata.type");
    }
    return entry;
  }

  public static String getType(SorobanMetadata metadata) {
    return metadata.getMetaString(KEY_TYPE);
  }

  public static <I extends SorobanItem> Predicate<I> filterByType(Class... types) {
    List<String> typesList =
        Arrays.stream(types).map(type -> type.getName()).collect(Collectors.toList());
    return item -> typesList.contains(getType(item.getMetadata()));
  }
}
