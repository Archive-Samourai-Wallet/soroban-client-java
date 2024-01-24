package com.samourai.soroban.client.endpoint.meta;

import org.json.JSONObject;

public class SorobanEntryMeta {
  private static final String KEY_PAYLOAD = "payload";
  private static final String KEY_METADATA = "metadata";

  private String payload;
  private SorobanMetadata metadata;

  public SorobanEntryMeta(String payload) {
    this.payload = payload;
    this.metadata = new SorobanMetadataImpl();
  }

  public SorobanEntryMeta(JSONObject jsonObject) throws Exception {
    this.payload = jsonObject.getString(KEY_PAYLOAD);
    JSONObject jsonObjectMeta = jsonObject.getJSONObject(KEY_METADATA);
    this.metadata = new SorobanMetadataImpl(jsonObjectMeta);
  }

  public String toPayload() {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(KEY_PAYLOAD, payload);
    jsonObject.put(KEY_METADATA, metadata.toJsonObject());
    return jsonObject.toString();
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  public SorobanMetadata getMetadata() {
    return metadata;
  }

  @Override
  public String toString() {
    return "SorobanEntryMeta{" + "payload='" + payload + '\'' + ", metadata=" + metadata + '}';
  }
}
