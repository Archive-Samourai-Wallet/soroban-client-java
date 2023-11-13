package com.samourai.soroban.client.meeting;

import com.samourai.soroban.client.SorobanPayload;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated // TODO
public class SorobanMessageWithSender implements SorobanPayload {
  private static final Logger log = LoggerFactory.getLogger(SorobanMessageWithSender.class);

  private String sender;
  private String payload;
  private long timePayload;

  public SorobanMessageWithSender(String sender, String payload, long timePayload) {
    this.sender = sender;
    this.payload = payload;
    this.timePayload = timePayload;
  }

  public SorobanMessageWithSender(String sender, String payload) {
    this(sender, payload, System.currentTimeMillis());
  }

  public static SorobanMessageWithSender parse(String payloadStr) throws Exception {
    JSONObject obj = new JSONObject(payloadStr);

    String sender = obj.getString("sender");
    String payload = obj.getString("payload");
    if (StringUtils.isEmpty(sender) || StringUtils.isEmpty(payload)) {
      throw new Exception("Invalid SorobanMessageWithSender");
    }
    return new SorobanMessageWithSender(sender, payload);
  }

  public static String toPayload(String sender, String payload) {
    JSONObject obj = new JSONObject();
    obj.put("sender", sender);
    obj.put("payload", payload);
    return obj.toString();
  }

  @Override
  public String toPayload() {
    return toPayload(sender, payload);
  }

  public String getTypePayload() {
    return SorobanMessageWithSender.class.getName();
  }

  public long getTimePayload() {
    return timePayload;
  }

  @Override
  public String toString() {
    return "SorobanMessageWithSender{"
        + "sender='"
        + sender
        + '\''
        + ", payload='"
        + payload
        + "\', timePayload="
        + timePayload
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
}
