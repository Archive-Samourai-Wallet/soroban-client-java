package com.samourai.soroban.client.exception;

import com.samourai.soroban.client.endpoint.meta.typed.SorobanItemTyped;
import java.util.Collection;

public class UnexpectedSorobanPayloadTypedException extends SorobanException {
  private SorobanItemTyped sorobanItemTyped;

  public UnexpectedSorobanPayloadTypedException(
      SorobanItemTyped sorobanItemTyped, Collection<String> typesExpected) {
    super(
        "Unexpected typePayload: expected="
            + typesExpected
            + ", actual="
            + sorobanItemTyped.getType());
    this.sorobanItemTyped = sorobanItemTyped;
  }

  public SorobanItemTyped getSorobanItemTyped() {
    return sorobanItemTyped;
  }
}
