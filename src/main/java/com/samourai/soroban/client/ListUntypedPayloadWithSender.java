package com.samourai.soroban.client;

import com.samourai.wallet.util.CallbackWithArg;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListUntypedPayloadWithSender extends ListUntypedPayload {
  private static final Logger log = LoggerFactory.getLogger(ListUntypedPayloadWithSender.class);

  public ListUntypedPayloadWithSender(Collection<UntypedPayloadWithSender> list) {
    super(list);
  }

  public ListUntypedPayloadWithSender distinctBySender() {
    // keep last payload for each sender
    this.list =
        getList().stream()
            .collect(
                Collectors.toMap(
                    p -> p.getSender().toString(),
                    p -> p,
                    (a, b) ->
                        a.getTimePayload() >= b.getTimePayload() ? a : b)) // keep last payload
            .values();
    return this;
  }

  public <T extends SorobanPayload> List<PayloadWithSender<T>> readListWithSender(Class<T> type) {
    return readListWithSender(type, null);
  }

  public <T extends SorobanPayload> List<PayloadWithSender<T>> readListWithSender(
      Class<T> type, Predicate<PayloadWithSender<T>> filterOrNull) {
    CallbackWithArg<UntypedPayloadWithSender, PayloadWithSender<T>> adapt =
        untypedPayload -> untypedPayload.readWithSender(type);
    return adaptList(getList(), adapt, filterOrNull);
  }

  public Collection<UntypedPayloadWithSender> getList() {
    return (Collection<UntypedPayloadWithSender>) list;
  }

  public Optional<UntypedPayloadWithSender> getFirst() {
    return (Optional<UntypedPayloadWithSender>) super.getFirst();
  }
}
