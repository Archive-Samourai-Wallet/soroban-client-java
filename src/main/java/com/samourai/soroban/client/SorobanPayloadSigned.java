package com.samourai.soroban.client;

public class SorobanPayloadSigned extends AbstractSorobanPayload {
  private String payload;
  private String signature;

  public SorobanPayloadSigned() {}

  public SorobanPayloadSigned(String payload, String signature) {
    this.payload = payload;
    this.signature = signature;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  public String getSignature() {
    return signature;
  }

  public void setSignature(String signature) {
    this.signature = signature;
  }
}
