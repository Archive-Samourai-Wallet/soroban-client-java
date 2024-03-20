package com.samourai.soroban.client.endpoint.meta.wrapper;

import com.samourai.soroban.client.endpoint.meta.SorobanMetadata;
import com.samourai.soroban.client.exception.FilterDeclinedSorobanException;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import com.samourai.wallet.util.Pair;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** Metadata: type */
public class SorobanWrapperMetaFilterType extends SorobanWrapperMetaType {
  public List<String> typesAllowed;

  public SorobanWrapperMetaFilterType(Class[] typesAllowed) {
    this.typesAllowed =
        Arrays.stream(typesAllowed).map(type -> type.getName()).collect(Collectors.toList());
  }

  protected boolean isTypeAllowed(String className) {
    return typesAllowed.contains(className);
  }

  @Override
  public Pair<String, SorobanMetadata> onSend(
      Bip47Encrypter encrypter, Pair<String, SorobanMetadata> entry, Object initialPayload)
      throws Exception {
    // check type
    String type = initialPayload.getClass().getName();
    if (!isTypeAllowed(type)) {
      throw newException(type);
    }

    // set type
    return super.onSend(encrypter, entry, initialPayload);
  }

  @Override
  public Pair<String, SorobanMetadata> onReceive(
      Bip47Encrypter encrypter, Pair<String, SorobanMetadata> entry) throws Exception {
    // require type
    entry = super.onReceive(encrypter, entry);

    // check type
    String type = getType(entry.getRight());
    if (!isTypeAllowed(type)) {
      throw newException(type);
    }
    return entry;
  }

  protected FilterDeclinedSorobanException newException(String type) {
    return new FilterDeclinedSorobanException(
        "Forbidden type: " + type + " vs " + Arrays.toString(typesAllowed.toArray()));
  }
}
