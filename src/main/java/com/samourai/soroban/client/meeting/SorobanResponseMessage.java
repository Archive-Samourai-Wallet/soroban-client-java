package com.samourai.soroban.client.meeting;

import com.samourai.soroban.client.dialog.AbstractSorobanMessage;
import org.json.JSONObject;

public class SorobanResponseMessage extends AbstractSorobanMessage {
  private boolean accept;

  public SorobanResponseMessage() {
    this(false);
  }

  public SorobanResponseMessage(boolean accept) {
    super(true);
    this.accept = accept;
  }

  public static SorobanResponseMessage parse(String payload) throws Exception {
    JSONObject obj = new JSONObject(payload);

    if (!obj.has("accept")) {
      throw new Exception("missing .accept");
    }
    boolean accept = obj.getBoolean("accept");
    return new SorobanResponseMessage(accept);
  }

  @Override
  public String toPayload() {
    JSONObject obj = new JSONObject();
    obj.put("accept", accept);
    return obj.toString();
  }

  @Override
  public boolean isDone() {
    return true;
  }

  public boolean isAccept() {
    return accept;
  }

  @Override
  public String toString() {
    return "accept=" + accept;
  }
}
