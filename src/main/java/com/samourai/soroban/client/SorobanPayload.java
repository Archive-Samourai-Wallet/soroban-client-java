package com.samourai.soroban.client;

import org.json.JSONObject;

public interface SorobanPayload extends SorobanPayloadable {
  String getSender();

  JSONObject getMetadata();

  String getPayload();

  void setPayload(String payload);
}
