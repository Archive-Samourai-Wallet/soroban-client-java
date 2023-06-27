package com.samourai.soroban.client.rpc;

public enum RpcMode {
  NORMAL("normal"),
  SHORT("short");

  private String value;

  RpcMode(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
