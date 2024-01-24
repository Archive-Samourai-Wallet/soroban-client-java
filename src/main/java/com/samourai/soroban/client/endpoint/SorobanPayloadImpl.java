package com.samourai.soroban.client.endpoint;

import com.samourai.soroban.client.SorobanPayload;
import com.samourai.soroban.client.exception.SorobanException;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

public class SorobanPayloadImpl implements SorobanPayload {
  private static final String KEY_SENDER = "sender";
  private static final String KEY_PAYLOAD = "payload";
  private static final String KEY_METADATA = "metadata";

  private JSONObject jsonObject;

  public SorobanPayloadImpl(String sender, String payload) {
    this.jsonObject = new JSONObject();
    setSender(sender);
    setPayload(payload);
  }

  public SorobanPayloadImpl(SorobanPayload sorobanPayload) {
    this(sorobanPayload.getSender(), sorobanPayload.getPayload());
    jsonObject.put(KEY_METADATA, sorobanPayload.getMetadata());
  }

  protected SorobanPayloadImpl(String json) throws Exception {
    this.jsonObject = new JSONObject(json);

    // validate sender
    String sender = getSender();
    if (StringUtils.isEmpty(sender)) {
      throw new SorobanException("Invalid sender: null");
    }
    try {
      new PaymentCode(sender);
    } catch (Exception e) {
      throw new SorobanException("Invalid sender: " + sender);
    }
  }

  @Override
  public String toPayload() throws Exception {
    return jsonObject.toString();
  }

  @Override
  public JSONObject getMetadata() {
    if (!jsonObject.has(KEY_METADATA)) {
      jsonObject.put(KEY_METADATA, new JSONObject());
    }
    return jsonObject.getJSONObject(KEY_METADATA);
  }

  @Override
  public String getSender() {
    return jsonObject.getString(KEY_SENDER);
  }

  private void setSender(String sender) {
    jsonObject.put(KEY_SENDER, sender);
  }

  @Override
  public String getPayload() {
    return jsonObject.getString(KEY_PAYLOAD);
  }

  @Override
  public void setPayload(String payload) {
    jsonObject.put(KEY_PAYLOAD, payload);
  }
}
