package com.samourai.soroban.client.exception;

import com.samourai.soroban.client.SorobanPayload;

public abstract class SorobanPayloadException extends SorobanException {
  private SorobanPayload sorobanPayload;

  public SorobanPayloadException(String message, SorobanPayload sorobanPayload) {
    super(message);
    this.sorobanPayload = sorobanPayload;
  }

  public SorobanPayloadException(SorobanPayload sorobanPayload) {
    this("SorobanPayloadException: " + sorobanPayload.getClass().getName(), sorobanPayload);
  }

  public SorobanPayloadException(String message) {
    super(message);
  }

  public SorobanPayload getSorobanPayload() {
    return sorobanPayload;
  }

  public void setSorobanPayload(SorobanPayload sorobanPayload) {
    this.sorobanPayload = sorobanPayload;
  }
}
