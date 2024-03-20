package com.samourai.soroban.client.exception;

import com.samourai.wallet.sorobanClient.SorobanPayloadable;

public abstract class SorobanPayloadException extends SorobanException {
  private SorobanPayloadable sorobanPayload;

  public SorobanPayloadException(String message, SorobanPayloadable sorobanPayload) {
    super(message);
    this.sorobanPayload = sorobanPayload;
  }

  public SorobanPayloadException(SorobanPayloadable sorobanPayload) {
    this("SorobanPayloadException: " + sorobanPayload.getClass().getName(), sorobanPayload);
  }

  public SorobanPayloadException(String message) {
    super(message);
  }

  public SorobanPayloadable getSorobanPayload() {
    return sorobanPayload;
  }

  public void setSorobanPayload(SorobanPayloadable sorobanPayload) {
    this.sorobanPayload = sorobanPayload;
  }
}
