package com.samourai.soroban.client;

import com.samourai.wallet.util.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSorobanPayload implements SorobanPayload {
  private static final Logger log = LoggerFactory.getLogger(AbstractSorobanPayload.class);
  private String typePayload;
  private Long timePayload;

  public AbstractSorobanPayload() {
    this.typePayload = null; // set by toPayload()
    this.timePayload = null; // set by toPayload()
  }

  @Override
  public String getTypePayload() {
    return typePayload;
  }

  @Override
  public long getTimePayload() {
    return timePayload;
  }

  @Override
  public String toPayload() {
    try {
      typePayload = getClass().getName(); // use subclass type
      timePayload = System.currentTimeMillis();
      return JSONUtils.getInstance().getObjectMapper().writeValueAsString(this);
    } catch (Exception e) {
      log.error("", e);
      return null;
    }
  }
}
