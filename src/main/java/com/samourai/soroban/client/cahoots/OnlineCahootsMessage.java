package com.samourai.soroban.client.cahoots;

import com.samourai.wallet.cahoots.Cahoots;
import com.samourai.wallet.cahoots.ManualCahootsMessage;
import org.json.JSONObject;

public class OnlineCahootsMessage extends ManualCahootsMessage {
  private boolean done;

  public OnlineCahootsMessage(Cahoots cahoots, boolean done) {
    super(cahoots);
    this.done = done;
  }

  public static OnlineCahootsMessage parse(String payload) throws Exception {
    JSONObject obj = new JSONObject(payload);

    if (!obj.has("cahoots")) {
      throw new Exception("missing .cahoots");
    }
    if (!obj.has("done")) {
      throw new Exception("missing .done");
    }

    Cahoots cahoots = Cahoots.parse(obj.getString("cahoots"));
    boolean done = obj.getBoolean("done");
    return new OnlineCahootsMessage(cahoots, done);
  }

  @Override
  public String toPayload() {
    JSONObject obj = new JSONObject();
    obj.put("cahoots", getCahoots().toJSONString());
    obj.put("done", done);
    return obj.toString();
  }

  @Override
  public boolean isDone() {
    return done;
  }
}
