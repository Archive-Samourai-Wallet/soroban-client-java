package com.samourai.soroban.client.rpc;

import com.samourai.soroban.client.AbstractSorobanPayloadable;

public class TestPayload extends AbstractSorobanPayloadable {
  private String message;

  public TestPayload() {}

  public TestPayload(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }
}
