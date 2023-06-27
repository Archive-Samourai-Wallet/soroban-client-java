package com.samourai.soroban.client.meeting;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanMessageWithSender {
  private static final Logger log = LoggerFactory.getLogger(SorobanMessageWithSender.class);

  private String sender;
  private String payload;
  private String initialPayload; // encrypted

  public SorobanMessageWithSender(String sender, String payload, String initialPayload) {
    this.sender = sender;
    this.payload = payload;
    this.initialPayload = initialPayload;
  }

  public static SorobanMessageWithSender parse(String payloadStr) throws Exception {
    JSONObject obj = new JSONObject(payloadStr);

    String sender = obj.getString("sender");
    String payload = obj.getString("payload");
    if (StringUtils.isEmpty(sender) || StringUtils.isEmpty(payload)) {
      throw new Exception("Invalid SorobanMessageWithSender");
    }
    return new SorobanMessageWithSender(sender, payload, payloadStr);
  }

  public static String toPayload(String sender, String payload) {
    JSONObject obj = new JSONObject();
    obj.put("sender", sender);
    obj.put("payload", payload);
    return obj.toString();
  }

  @Override
  public String toString() {
    return "SorobanMessageWithSender{"
        + "sender='"
        + sender
        + '\''
        + ", payload='"
        + payload
        + '\''
        + '}';
  }

  public String getSender() {
    return sender;
  }

  public void setSender(String sender) {
    this.sender = sender;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  public String getInitialPayload() {
    return initialPayload;
  }

  public void setInitialPayload(String initialPayload) {
    this.initialPayload = initialPayload;
  }
}
