package com.samourai.soroban.client;

public class SorobanPayloadSignedWithSender extends SorobanPayloadSigned {
  private String sender;

  public SorobanPayloadSignedWithSender() {}

  public SorobanPayloadSignedWithSender(String payload, String signature, String sender) {
    super(payload, signature);
    this.sender = sender;
  }

  public String getSender() {
    return sender;
  }

  public void setSender(String sender) {
    this.sender = sender;
  }
}
