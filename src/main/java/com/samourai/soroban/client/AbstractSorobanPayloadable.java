package com.samourai.soroban.client;

import com.samourai.wallet.sorobanClient.SorobanPayloadable;
import com.samourai.wallet.util.JSONUtils;

public class AbstractSorobanPayloadable implements SorobanPayloadable {
  protected static final JSONUtils jsonUtils = JSONUtils.getInstance();

  public AbstractSorobanPayloadable() {}

  @Override
  public String toPayload() throws Exception {
    return jsonUtils.getObjectMapper().writeValueAsString(this);
  }
}
