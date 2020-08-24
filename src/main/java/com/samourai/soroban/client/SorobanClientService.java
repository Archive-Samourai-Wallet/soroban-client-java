package com.samourai.soroban.client;

import com.samourai.http.client.IHttpClient;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.cahoots.CahootsMessage;
import com.samourai.wallet.cahoots.CahootsService;
import com.samourai.wallet.cahoots.CahootsWallet;
import com.samourai.wallet.soroban.client.SorobanMessage;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import java8.util.Optional;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanClientService {
  private final Logger log = LoggerFactory.getLogger(SorobanClientService.class);

  private static final int LOOP_DELAY_MS = 1 * 1000;

  private CahootsService cahootsService;
  private SorobanService sorobanService;
  private int timeoutMs;
  private Optional<SorobanOrchestrator> sorobanOrchestrator;
  private Subject<SorobanMessage> onMessage;
  private Subject<SorobanMessage> onResult;

  public SorobanClientService(
      NetworkParameters params,
      CahootsWallet cahootsWallet,
      BIP47Wallet bip47Wallet,
      int account,
      IHttpClient httpClient,
      int timeoutMs) {
    this.cahootsService = new CahootsService(params, cahootsWallet, account);
    this.sorobanService =
        new SorobanService(params, bip47Wallet, cahootsService, httpClient, timeoutMs);
    this.timeoutMs = timeoutMs;
    this.sorobanOrchestrator = Optional.empty();
    this.onMessage = BehaviorSubject.create();
    this.onResult = BehaviorSubject.create();
  }

  public boolean isStartedListening() {
    return sorobanOrchestrator.isPresent();
  }

  public synchronized void startListening(PaymentCode paymentCodeInitiator)
      throws Exception { // TODO ZL
    if (isStartedListening()) {
      throw new Exception("SorobanClientService already listening");
    }
    sorobanOrchestrator =
        Optional.of(
            new SorobanOrchestrator(
                LOOP_DELAY_MS,
                sorobanService,
                paymentCodeInitiator,
                onMessage,
                onResult,
                timeoutMs));
    sorobanOrchestrator.get().start(true);
  }

  public synchronized void stopListening() throws Exception {
    if (!sorobanOrchestrator.isPresent()) {
      throw new Exception("SorobanClientService was not started");
    }
    sorobanOrchestrator.get().stop();
    sorobanOrchestrator = Optional.empty();
  }

  public Subject<SorobanMessage> newStonewallx2(
      long amount, String address, PaymentCode paymentCodeCounterparty) throws Exception {
    CahootsMessage message = cahootsService.newStonewallx2(amount, address);
    Subject<SorobanMessage> onMessage = BehaviorSubject.create();
    sorobanService.initiator(paymentCodeCounterparty, message, onMessage);
    return onMessage;
  }

  public Subject<SorobanMessage> newStowaway(long amount, PaymentCode paymentCodeCounterparty)
      throws Exception {
    CahootsMessage message = cahootsService.newStowaway(amount);
    Subject<SorobanMessage> onMessage = BehaviorSubject.create();
    sorobanService.initiator(paymentCodeCounterparty, message, onMessage);
    return onMessage;
  }

  public Subject<SorobanMessage> getOnMessage() {
    return onMessage;
  }

  public Subject<SorobanMessage> getOnResult() {
    return onResult;
  }

  public SorobanService getSorobanService() {
    return sorobanService;
  }

  public CahootsService getCahootsService() {
    return cahootsService;
  }
}
