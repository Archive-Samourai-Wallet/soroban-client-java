package com.samourai.soroban.client.cahoots;

import com.samourai.wallet.cahoots.Cahoots;
import com.samourai.wallet.cahoots.ManualCahootsMessage;
import org.json.JSONObject;

public class OnlineCahootsMessage extends ManualCahootsMessage {
  private boolean lastMessage;

  public OnlineCahootsMessage(Cahoots cahoots, boolean lastMessage) {
    super(cahoots);
    this.lastMessage = lastMessage;
  }

  public static OnlineCahootsMessage parse(String payload) throws Exception {
    JSONObject obj = new JSONObject(payload);

    if (!obj.has("cahoots")) {
      throw new Exception("missing .cahoots");
    }
    if (!obj.has("lastMessage")) {
      throw new Exception("missing .lastMessage");
    }

    Cahoots cahoots = Cahoots.parse(obj.getString("cahoots"));
    boolean lastMessage = obj.getBoolean("lastMessage");
    return new OnlineCahootsMessage(cahoots, lastMessage);
  }

  @Override
  public String toPayload() {
    JSONObject obj = new JSONObject();
    obj.put("cahoots", getCahoots().toJSONString());
    obj.put("lastMessage", lastMessage);
    return obj.toString();
  }

  @Override
  public boolean isDone() {
    return lastMessage;
  }

  public void setLastMessage(boolean lastMessage) {
    this.lastMessage = lastMessage;
  }
}
