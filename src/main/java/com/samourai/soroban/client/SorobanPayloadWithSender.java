package com.samourai.soroban.client;

public class SorobanPayloadWithSender extends AbstractSorobanPayload {
  private byte[] payload;
  private String sender;

  public SorobanPayloadWithSender() {}

  public SorobanPayloadWithSender(byte[] payload, String sender) {
    this.sender = sender;
    this.payload = payload;
  }

  public byte[] getPayload() {
    return payload;
  }

  public void setPayload(byte[] payload) {
    this.payload = payload;
  }

  public String getSender() {
    return sender;
  }

  public void setSender(String sender) {
    this.sender = sender;
  }
}
