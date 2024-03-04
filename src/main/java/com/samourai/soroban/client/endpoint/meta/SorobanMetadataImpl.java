package com.samourai.soroban.client.endpoint.meta;

import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaNonce;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaSender;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaType;
import com.samourai.wallet.bip47.rpc.PaymentCode;
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
    String type = SorobanWrapperMetaType.getType(this);
    PaymentCode sender = SorobanWrapperMetaSender.getSender(this);
    Long nonce = SorobanWrapperMetaNonce.getNonce(this);
    String str = jsonObject.toString();
    if (sender != null) {
      str += " sender=" + sender;
    }
    if (type != null) {
      str += " typePayload=" + type;
    }
    if (nonce != null) {
      str += " nonce=" + nonce;
    }
    return str;
  }
}
