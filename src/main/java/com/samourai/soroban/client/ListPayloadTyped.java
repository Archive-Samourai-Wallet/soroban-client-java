package com.samourai.soroban.client;

import com.samourai.wallet.util.CallbackWithArg;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListPayloadTyped {
  private static final Logger log = LoggerFactory.getLogger(ListPayloadTyped.class);
  protected Collection<? extends SorobanPayloadTyped> list;

  public ListPayloadTyped(Collection<? extends SorobanPayloadTyped> list) {
    this.list = list;
  }

  public <T> List<T> readList(CallbackWithArg<SorobanPayloadTyped, T> adapt) {
    return adaptList(list, adapt, null);
  }

  public <T extends SorobanPayloadable> List<T> readList(Class<T> type) {
    return readList(type, null);
  }

  public <T extends SorobanPayloadable> List<T> readList(Class<T> type, Predicate<T> filterOrNull) {
    CallbackWithArg<SorobanPayloadTyped, T> adapt = untypedPayload -> untypedPayload.read(type);
    return adaptList(list, adapt, filterOrNull);
  }

  public static <T, P> List<T> adaptList(
      Collection<? extends P> list, CallbackWithArg<P, T> adapt, Predicate<T> filterOrNull) {
    List<T> results = new LinkedList<>();
    for (P payload : list) {
      try {
        T item = adapt.apply(payload);
        if (filterOrNull == null || filterOrNull.test(item)) {
          results.add(item);
        }
      } catch (Exception e) {
        if (log.isDebugEnabled()) {
          log.error(
              "readList: skipping invalid payload, cause="
                  + e.getMessage()
                  + ", payload="
                  + payload,
              e);
        }
      }
    }
    return results;
  }

  public ListPayloadTyped distinctBySender() {
    // keep last payload for each sender
    this.list =
        list.stream()
            .collect(
                Collectors.toMap(
                    p -> p.getSender().toString(),
                    p -> p,
                    (a, b) ->
                        a.getTimePayload() >= b.getTimePayload() ? a : b)) // keep last payload
            .values();
    return this;
  }

  public Collection<? extends SorobanPayloadTyped> getList() {
    return list;
  }

  public int size() {
    return list.size();
  }

  public boolean isEmpty() {
    return list.isEmpty();
  }

  public Optional<? extends SorobanPayloadTyped> getFirst() {
    return isEmpty() ? Optional.empty() : Optional.of(list.iterator().next());
  }
}
