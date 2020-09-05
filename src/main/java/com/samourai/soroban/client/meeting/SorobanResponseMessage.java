package com.samourai.soroban.client.meeting;

import com.samourai.soroban.client.AbstractSorobanMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanResponseMessage extends AbstractSorobanMessage {
  private static final Logger log = LoggerFactory.getLogger(SorobanResponseMessage.class);

  private boolean accept;

  public SorobanResponseMessage() {
    this(false);
  }

  public SorobanResponseMessage(boolean accept) {
    super(true);
    this.accept = accept;
  }

  public static SorobanResponseMessage parse(String payload) throws Exception {
    return parse(payload, SorobanResponseMessage.class);
  }

  @Override
  public boolean isLastMessage() {
    return true;
  }

  public boolean isAccept() {
    return accept;
  }

  public void setAccept(boolean accept) {
    this.accept = accept;
  }

  @Override
  public String toString() {
    return "accept=" + accept;
  }
}
