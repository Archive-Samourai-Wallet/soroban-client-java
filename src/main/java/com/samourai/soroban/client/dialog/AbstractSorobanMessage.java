package com.samourai.soroban.client.dialog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samourai.wallet.soroban.client.SorobanMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSorobanMessage implements SorobanMessage {
  private static final Logger log = LoggerFactory.getLogger(AbstractSorobanMessage.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private boolean lastMessage;

  public AbstractSorobanMessage(boolean lastMessage) {
    this.lastMessage = lastMessage;
  }

  @Override
  public boolean isLastMessage() {
    return lastMessage;
  }

  public void setLastMessage(boolean lastMessage) {
    this.lastMessage = lastMessage;
  }

  protected static ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  public static <T extends SorobanMessage> T parse(String payload, Class<T> type) throws Exception {
    return objectMapper.readValue(payload, type);
  }

  @Override
  public String toPayload() {
    try {
      return objectMapper.writeValueAsString(this);
    } catch (Exception e) {
      log.error("", e);
      return null;
    }
  }
}
