package com.samourai.soroban.client.rpc;

public enum RpcMode {
  NORMAL("normal", 180000), // 3min
  SHORT("short", 30000); // 30s

  private String value;
  private long expirationMs;

  RpcMode(String value, long expirationMs) {
    this.value = value;
    this.expirationMs = expirationMs;
  }

  public String getValue() {
    return value;
  }

  public long getExpirationMs() {
    return expirationMs;
  }
}
