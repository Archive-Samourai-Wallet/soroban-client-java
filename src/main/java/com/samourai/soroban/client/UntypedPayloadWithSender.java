package com.samourai.soroban.client;

import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.util.Util;
import io.reactivex.functions.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UntypedPayloadWithSender extends UntypedPayload {
  private static final Logger log = LoggerFactory.getLogger(UntypedPayloadWithSender.class);
  private PaymentCode sender;

  public UntypedPayloadWithSender(String payload, PaymentCode sender) {
    super(payload);
    this.sender = sender;
  }

  public <T extends SorobanPayload> PayloadWithSender<T> readOnWithSender(Class<T> type)
      throws Exception {
    return readOnWithSender(type, null);
  }

  public <T extends SorobanPayload> PayloadWithSender<T> readOnWithSender(
      Class<T> type, Consumer<PayloadWithSender<T>> consumerOrNull) throws Exception {
    T res = readOn(type);
    if (res == null) {
      return null;
    }
    PayloadWithSender<T> payloadWithSender = new PayloadWithSender<>(res, sender);
    if (consumerOrNull != null) {
      consumerOrNull.accept(payloadWithSender);
    }
    return payloadWithSender;
  }

  public <T extends SorobanPayload> PayloadWithSender<T> readWithSender(Class<T> type)
      throws Exception {
    T o = super.read(type);
    return new PayloadWithSender<>(o, sender);
  }

  public PaymentCode getSender() {
    return sender;
  }

  public String computeUniqueId() {
    return sender.toString() + "/" + Util.sha256Hex(getPayload());
  }
}
