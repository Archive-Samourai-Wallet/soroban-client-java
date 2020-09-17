package com.samourai.soroban.client.pingPong;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samourai.wallet.soroban.client.SorobanMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PingPongMessage implements SorobanMessage {
  private static final Logger log = LoggerFactory.getLogger(PingPongMessage.class);

  private static final ObjectMapper objectMapper = new ObjectMapper();

  public enum VALUES {
    PING,
    PONG
  };

  private VALUES value;
  private int iteration;
  private boolean lastMessage;

  public PingPongMessage() {
    this.value = null;
    this.iteration = 0;
    this.lastMessage = false;
  }

  public PingPongMessage(VALUES value, boolean lastMessage, int iteration) {
    this.value = value;
    this.iteration = iteration;
    this.lastMessage = lastMessage;
  }

  public PingPongMessage(VALUES value, boolean lastMessage) {
    this(value, lastMessage, 1);
  }

  public static PingPongMessage parse(String payload) throws Exception {
    return objectMapper.readValue(payload, PingPongMessage.class);
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

  public VALUES getValue() {
    return value;
  }

  public void setValue(VALUES value) {
    this.value = value;
  }

  public int getIteration() {
    return iteration;
  }

  public void setIteration(int iteration) {
    this.iteration = iteration;
  }

  @Override
  public boolean isDone() {
    return lastMessage;
  }

  public void setLastMessage(boolean lastMessage) {
    this.lastMessage = lastMessage;
  }

  @JsonIgnore
  @Override
  public boolean isInteraction() {
    return false;
  }

  @Override
  public String toString() {
    return "value=" + value + ", iteration=" + iteration + ", lastMessage=" + lastMessage;
  }
}
