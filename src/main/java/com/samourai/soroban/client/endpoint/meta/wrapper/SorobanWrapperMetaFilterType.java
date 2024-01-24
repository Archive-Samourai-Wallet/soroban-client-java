package com.samourai.soroban.client.endpoint.meta.wrapper;

import com.samourai.soroban.client.endpoint.meta.SorobanEntryMeta;
import com.samourai.soroban.client.exception.FilterDeclinedSorobanException;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
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
  public SorobanEntryMeta onSend(
      Bip47Encrypter encrypter, SorobanEntryMeta sorobanEntry, Object initialPayload)
      throws Exception {
    // check type
    String type = initialPayload.getClass().getName();
    if (!isTypeAllowed(type)) {
      throw newException(type);
    }

    // set type
    return super.onSend(encrypter, sorobanEntry, initialPayload);
  }

  @Override
  public SorobanEntryMeta onReceive(Bip47Encrypter encrypter, SorobanEntryMeta sorobanEntry)
      throws Exception {
    // require type
    sorobanEntry = super.onReceive(encrypter, sorobanEntry);

    // check type
    String type = getType(sorobanEntry.getMetadata());
    if (!isTypeAllowed(type)) {
      throw newException(type);
    }
    return sorobanEntry;
  }

  protected FilterDeclinedSorobanException newException(String type) {
    return new FilterDeclinedSorobanException(
        "Forbidden type: " + type + " vs " + Arrays.toString(typesAllowed.toArray()));
  }
}
