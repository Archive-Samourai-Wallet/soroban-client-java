package com.samourai.soroban.client.pingPong;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samourai.soroban.client.AbstractSorobanPayloadable;
import com.samourai.soroban.client.SorobanInteraction;
import com.samourai.soroban.client.SorobanMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PingPongMessage extends AbstractSorobanPayloadable implements SorobanMessage {
  private static final Logger log = LoggerFactory.getLogger(PingPongMessage.class);

  private static final ObjectMapper objectMapper = new ObjectMapper();

  public enum VALUES {
    PING,
    PONG
  };

  private VALUES value;
  private int iteration;
  private boolean done;

  public PingPongMessage() {
    this.value = null;
    this.iteration = 0;
    this.done = false;
  }

  public PingPongMessage(VALUES value, boolean done, int iteration) {
    this.value = value;
    this.iteration = iteration;
    this.done = done;
  }

  public PingPongMessage(VALUES value, boolean done) {
    this(value, done, 1);
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
    return done;
  }

  public void setDone(boolean done) {
    this.done = done;
  }

  @JsonIgnore
  public SorobanInteraction getInteraction() {
    return null;
  }

  @Override
  public String toString() {
    return "value=" + value + ", iteration=" + iteration + ", done=" + done;
  }
}
