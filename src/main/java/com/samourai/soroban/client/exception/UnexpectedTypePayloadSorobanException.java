package com.samourai.soroban.client.exception;

import com.samourai.soroban.client.SorobanPayloadTyped;

public class UnexpectedTypePayloadSorobanException extends SorobanException {
  private SorobanPayloadTyped payload;

  public UnexpectedTypePayloadSorobanException(
      SorobanPayloadTyped untypedPayload, Class typeExpected) {
    super(
        "Unexpected typePayload: expected="
            + typeExpected.getName()
            + ", actual="
            + untypedPayload.getTypePayload());
    this.payload = payload;
  }

  public SorobanPayloadTyped getPayload() {
    return payload;
  }
}
