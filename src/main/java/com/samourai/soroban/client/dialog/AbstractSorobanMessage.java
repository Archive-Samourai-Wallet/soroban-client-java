package com.samourai.soroban.client.dialog;

import com.samourai.soroban.client.AbstractSorobanPayloadable;
import com.samourai.wallet.sorobanClient.SorobanMessage;

public abstract class AbstractSorobanMessage extends AbstractSorobanPayloadable
    implements SorobanMessage {

  private boolean done;

  public AbstractSorobanMessage(boolean done) {
    this.done = done;
  }

  @Override
  public boolean isDone() {
    return done;
  }
}
