package com.samourai.soroban.client;

import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.soroban.client.SorobanMessage;
import com.samourai.wallet.util.AbstractOrchestrator;
import io.reactivex.subjects.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanOrchestrator extends AbstractOrchestrator {
  private final Logger log = LoggerFactory.getLogger(SorobanOrchestrator.class);
  private final SorobanService sorobanService;
  private final PaymentCode paymentCodeInitiator; // TODO ZL
  private Subject<SorobanMessage> onMessage;
  private Subject<SorobanMessage> onResult;
  private int timeoutMs;

  public SorobanOrchestrator(
      int loopDelay,
      SorobanService sorobanService,
      PaymentCode paymentCodeInitiator,
      Subject<SorobanMessage> onMessage,
      Subject<SorobanMessage> onResult,
      int timeoutMs) {
    super(loopDelay, 0, null);
    this.sorobanService = sorobanService;
    this.paymentCodeInitiator = paymentCodeInitiator;
    this.onMessage = onMessage;
    this.onResult = onResult;
    this.timeoutMs = timeoutMs;
  }

  @Override
  protected void runOrchestrator() {
    if (log.isDebugEnabled()) {
      log.debug("Checking for #Cahoots requests...");
    }
    try {
      // check once per orchestrator iteration
      SorobanMessage lastMessage =
          sorobanService.contributor(paymentCodeInitiator, timeoutMs, onMessage);
      onResult.onNext(lastMessage);
    } catch (Exception e) {
      log.error("", e);
      onResult.onError(e);
    }
  }

  @Override
  public synchronized void stop() {
    sorobanService.close();
    super.stop();
  }
}
