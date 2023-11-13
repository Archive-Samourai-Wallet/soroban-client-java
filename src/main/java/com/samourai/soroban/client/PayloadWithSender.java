package com.samourai.soroban.client;

import com.samourai.wallet.bip47.rpc.PaymentCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PayloadWithSender<T extends SorobanPayload> {
  private static final Logger log = LoggerFactory.getLogger(PayloadWithSender.class);
  private T payload;
  private PaymentCode sender; // may be null

  public PayloadWithSender(T payload, PaymentCode sender) {
    this.payload = payload;
    this.sender = sender;
  }

  public T getPayload() {
    return payload;
  }

  public PaymentCode getSender() {
    return sender;
  }
}
