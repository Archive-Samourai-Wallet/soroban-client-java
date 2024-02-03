package com.samourai.soroban.client.endpoint.meta;

import org.json.JSONObject;

public class SorobanMetadataImpl implements SorobanMetadata {
  protected JSONObject jsonObject;

  public SorobanMetadataImpl() {
    this(new JSONObject());
  }

  public SorobanMetadataImpl(JSONObject jsonObject) {
    this.jsonObject = jsonObject;
  }

  public SorobanMetadataImpl(SorobanMetadataImpl copy) {
    this.jsonObject = new JSONObject(copy.jsonObject.toString());
  }

  @Override
  public String getMetaString(String key) {
    if (!jsonObject.has(key)) {
      return null;
    }
    return jsonObject.getString(key);
  }

  @Override
  public Long getMetaLong(String key) {
    if (!jsonObject.has(key)) {
      return null;
    }
    return jsonObject.getLong(key);
  }

  @Override
  public void setMeta(String key, String value) {
    jsonObject.put(key, value);
  }

  @Override
  public void setMeta(String key, Long value) {
    jsonObject.put(key, value);
  }

  @Override
  public JSONObject toJsonObject() {
    return jsonObject;
  }

  @Override
  public String toString() {
    return jsonObject.toString();
  }
}
