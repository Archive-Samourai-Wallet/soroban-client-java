package com.samourai.soroban.client.rpc;

import com.samourai.soroban.client.AbstractSorobanPayloadable;

public class AnotherTestPayload extends AbstractSorobanPayloadable {
  private String info;

  public AnotherTestPayload() {}

  public AnotherTestPayload(String info) {
    this.info = info;
  }

  public String getInfo() {
    return info;
  }
}
