package com.samourai.soroban.client.endpoint.meta;

import org.json.JSONObject;

public interface SorobanMetadata {
  String getMetaString(String key);

  Long getMetaLong(String key);

  void setMeta(String key, String value);

  void setMeta(String key, Long value);

  JSONObject toJsonObject();
}
