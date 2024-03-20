package com.samourai.soroban.client.meeting;

import com.samourai.soroban.client.dialog.AbstractSorobanMessage;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.cahoots.CahootsType;
import org.json.JSONObject;

public class SorobanRequestMessage extends AbstractSorobanMessage {
  private CahootsType type;
  private PaymentCode sender;

  public SorobanRequestMessage() {
    this(null, null);
  }

  public SorobanRequestMessage(CahootsType type, PaymentCode sender) {
    super(true);
    this.type = type;
    this.sender = sender;
  }

  public static SorobanRequestMessage parse(String payload, PaymentCode sender) throws Exception {
    JSONObject obj = new JSONObject(payload);

    if (!obj.has("type")) {
      throw new Exception("missing .type");
    }
    CahootsType type = CahootsType.find(obj.getInt("type")).get();
    return new SorobanRequestMessage(type, sender);
  }

  @Override
  public String toPayload() {
    JSONObject obj = new JSONObject();
    obj.put("type", type.getValue());
    // 'sender' is transmitted in SorobanMessageWithSender
    return obj.toString();
  }

  @Override
  public boolean isDone() {
    return true;
  }

  public CahootsType getType() {
    return type;
  }

  public PaymentCode getSender() {
    return sender;
  }

  public void setSender(PaymentCode sender) {
    this.sender = sender;
  }

  @Override
  public String toString() {
    return "SorobanRequestMessage{" + "typePayload=" + type + ", sender=" + sender + '}';
  }
}
