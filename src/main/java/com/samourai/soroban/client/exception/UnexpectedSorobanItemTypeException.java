package com.samourai.soroban.client.exception;

import com.samourai.soroban.client.endpoint.meta.typed.SorobanItemTyped;
import java.util.Arrays;
import java.util.stream.Collectors;

public class UnexpectedSorobanItemTypeException extends SorobanException {
  private SorobanItemTyped sorobanItemTyped;

  public UnexpectedSorobanItemTypeException(
      SorobanItemTyped sorobanItemTyped, Class[] typesExpected) {
    super(
        "Unexpected SorobanItem type: expected="
            + Arrays.stream(typesExpected).map(c -> c.getName()).collect(Collectors.joining(", "))
            + ", actual="
            + sorobanItemTyped.getType());
    this.sorobanItemTyped = sorobanItemTyped;
  }

  public SorobanItemTyped getSorobanItemTyped() {
    return sorobanItemTyped;
  }
}
