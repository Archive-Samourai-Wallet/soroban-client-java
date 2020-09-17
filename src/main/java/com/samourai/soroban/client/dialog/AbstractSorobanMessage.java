package com.samourai.soroban.client.dialog;

import com.samourai.wallet.soroban.client.SorobanMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSorobanMessage implements SorobanMessage {
  private static final Logger log = LoggerFactory.getLogger(AbstractSorobanMessage.class);

  private boolean lastMessage;

  public AbstractSorobanMessage(boolean lastMessage) {
    this.lastMessage = lastMessage;
  }

  @Override
  public boolean isDone() {
    return lastMessage;
  }

  @Override
  public boolean isInteraction() {
    return false;
  }
}
