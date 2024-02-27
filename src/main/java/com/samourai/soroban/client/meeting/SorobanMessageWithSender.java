package com.samourai.soroban.client.meeting;

import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.sorobanClient.SorobanPayloadable;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

public class SorobanMessageWithSender implements SorobanPayloadable {

  private PaymentCode sender;
  private String payload;
  private String rawEntry; // encrypted SorobanMessageWithSender.payload

  public SorobanMessageWithSender(PaymentCode sender, String payload, String rawEntry) {
    this.sender = sender;
    this.payload = payload;
    this.rawEntry = rawEntry;
  }

  public static SorobanMessageWithSender parse(String payloadStr) throws Exception {
    JSONObject obj = new JSONObject(payloadStr);

    String sender = obj.getString("sender");
    String payload = obj.getString("payload");
    if (StringUtils.isEmpty(sender) || StringUtils.isEmpty(payload)) {
      throw new Exception("Invalid SorobanMessageWithSender");
    }
    return new SorobanMessageWithSender(new PaymentCode(sender), payload, null);
  }

  public static String toPayload(PaymentCode sender, String payload) {
    JSONObject obj = new JSONObject();
    obj.put("sender", sender.toString());
    obj.put("payload", payload);
    return obj.toString();
  }

  @Override
  public String toPayload() {
    return toPayload(sender, payload);
  }

  @Override
  public String toString() {
    return "SorobanMessageWithSender{"
        + "sender='"
        + sender.toString()
        + '\''
        + ", payload='"
        + payload
        + "\'}";
  }

  public PaymentCode getSender() {
    return sender;
  }

  public void setSender(PaymentCode sender) {
    this.sender = sender;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  public String getRawEntry() {
    return rawEntry;
  }
}
