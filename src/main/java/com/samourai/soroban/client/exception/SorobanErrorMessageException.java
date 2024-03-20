package com.samourai.soroban.client.exception;

import com.samourai.soroban.protocol.payload.SorobanErrorMessage;

public class SorobanErrorMessageException extends SorobanPayloadException {
  public SorobanErrorMessageException(SorobanErrorMessage sorobanErrorMessage) {
    super(
        "Error " + sorobanErrorMessage.errorCode + ": " + sorobanErrorMessage.message,
        sorobanErrorMessage);
  }

  public SorobanErrorMessage getSorobanErrorMessage() {
    return (SorobanErrorMessage) getSorobanPayload();
  }
}
