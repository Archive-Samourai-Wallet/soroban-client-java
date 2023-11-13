package com.samourai.soroban.client.exception;

import com.samourai.soroban.client.UntypedPayload;

public class UnexpectedTypePayloadSorobanException extends SorobanException {
  private UntypedPayload payload;

  public UnexpectedTypePayloadSorobanException(UntypedPayload untypedPayload, Class typeExpected) {
    super(
        "Unexpected typePayload: expected="
            + typeExpected.getName()
            + ", actual="
            + untypedPayload.getTypePayload());
    this.payload = payload;
  }

  public UntypedPayload getPayload() {
    return payload;
  }
}
