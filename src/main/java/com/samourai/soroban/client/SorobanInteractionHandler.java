package com.samourai.soroban.client;

import com.samourai.wallet.sorobanClient.SorobanMessage;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanInteractionHandler {
  private static final Logger log = LoggerFactory.getLogger(SorobanInteractionHandler.class);

  private Subject<SorobanMessage> interactiveMessageProvider;
  private Subject<OnlineSorobanInteraction> onInteraction;

  public SorobanInteractionHandler(Consumer<OnlineSorobanInteraction> doOnInteraction) {
    this.interactiveMessageProvider = BehaviorSubject.create();
    this.onInteraction = BehaviorSubject.create();
    onInteraction.subscribeOn(Schedulers.io()).subscribe(doOnInteraction);
  }

  void replyInteractive(SorobanMessage message) {
    if (log.isDebugEnabled()) {
      log.debug(" => replyInteractive");
    }
    interactiveMessageProvider.onNext(message);
  }

  void replyInteractive(Exception e) {
    if (log.isDebugEnabled()) {
      log.debug(" => replyInteractive reject: " + e.getMessage());
    }
    interactiveMessageProvider.onError(e);
    interactiveMessageProvider.onComplete();
  }

  public Subject<SorobanMessage> getInteractiveMessageProvider() {
    return interactiveMessageProvider;
  }

  public Subject<OnlineSorobanInteraction> getOnInteraction() {
    return onInteraction;
  }
}
