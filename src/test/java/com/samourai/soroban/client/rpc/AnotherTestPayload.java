package com.samourai.soroban.client.rpc;

import com.samourai.soroban.client.AbstractSorobanPayload;

public class AnotherTestPayload extends AbstractSorobanPayload {
  private String info;

  public AnotherTestPayload() {}

  public AnotherTestPayload(String info) {
    this.info = info;
  }

  public String getInfo() {
    return info;
  }
}
