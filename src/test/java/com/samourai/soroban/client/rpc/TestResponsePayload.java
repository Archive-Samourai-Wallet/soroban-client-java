package com.samourai.soroban.client.rpc;

import com.samourai.soroban.client.AbstractSorobanPayloadable;

public class TestResponsePayload extends AbstractSorobanPayloadable {
  private String responseMessage;

  public TestResponsePayload() {}

  public TestResponsePayload(String responseMessage) {
    this.responseMessage = responseMessage;
  }

  public String getResponseMessage() {
    return responseMessage;
  }
}
