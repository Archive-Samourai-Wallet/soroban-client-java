package com.samourai.soroban.client.pingPong;

import com.samourai.wallet.sorobanClient.SorobanContext;
import com.samourai.wallet.sorobanClient.SorobanMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PingPongService extends SorobanMessageService<PingPongMessage, SorobanContext> {
  private static final Logger log = LoggerFactory.getLogger(PingPongService.class);

  private int iterations;

  public PingPongService(int iterations) {
    this.iterations = iterations;
  }

  @Override
  public PingPongMessage parse(String payload) throws Exception {
    return PingPongMessage.parse(payload);
  }

  @Override
  public PingPongMessage reply(SorobanContext sorobanContext, PingPongMessage message) {
    PingPongMessage.VALUES value =
        PingPongMessage.VALUES.PING.equals(message.getValue())
            ? PingPongMessage.VALUES.PONG
            : PingPongMessage.VALUES.PING;
    int iteration = message.getIteration() + 1;
    boolean last = iteration == iterations;
    return new PingPongMessage(value, last, iteration);
  }
}
