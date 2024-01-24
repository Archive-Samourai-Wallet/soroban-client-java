package com.samourai.soroban.client.endpoint.typed;

import com.samourai.soroban.client.SorobanPayload;
import com.samourai.soroban.client.SorobanPayloadable;
import com.samourai.soroban.client.endpoint.SorobanPayloadImpl;
import com.samourai.soroban.client.exception.SorobanException;
import com.samourai.wallet.util.JSONUtils;

public class SorobanPayloadTyped<T extends SorobanPayloadable> extends SorobanPayloadImpl {
  private static final JSONUtils jsonUtils = JSONUtils.getInstance();
  private static final String META_TYPED = "typed";

  private T payloadTyped;

  public SorobanPayloadTyped(String sender, T payload) throws Exception {
    super(sender, payload.toPayload());
    this.payloadTyped = payload;
    getMetadata().put(META_TYPED, payload.getClass().getName());
  }

  public SorobanPayloadTyped(SorobanPayload sorobanPayload) {
    super(sorobanPayload);
  }

  public SorobanPayloadTyped(String json) throws Exception {
    super(json);
  }

  public T getPayloadTyped() throws Exception {
    if (payloadTyped == null) {
      String typed = getMetadata().getString(META_TYPED);
      try {
        Class<T> typedClass = (Class<T>) Class.forName(typed);
        payloadTyped = jsonUtils.getObjectMapper().readValue(getPayload(), typedClass);
      } catch (Exception e) {
        throw new SorobanException("Could not parse payloadTyped: " + typed);
      }
    }
    return payloadTyped;
  }
}
