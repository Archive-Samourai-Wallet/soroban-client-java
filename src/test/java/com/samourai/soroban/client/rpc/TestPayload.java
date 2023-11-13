package com.samourai.soroban.client.rpc;

import com.samourai.soroban.client.AbstractSorobanPayload;

public class TestPayload extends AbstractSorobanPayload {
  private String message;

  public TestPayload() {}

  public TestPayload(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }
}
