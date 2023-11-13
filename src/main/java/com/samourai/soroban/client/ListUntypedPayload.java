package com.samourai.soroban.client;

import com.samourai.wallet.util.CallbackWithArg;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListUntypedPayload {
  private static final Logger log = LoggerFactory.getLogger(ListUntypedPayload.class);
  protected Collection<? extends UntypedPayload> list;

  public ListUntypedPayload(Collection<? extends UntypedPayload> list) {
    this.list = list;
  }

  public <T> List<T> readList(CallbackWithArg<UntypedPayload, T> adapt) {
    return adaptList(list, adapt, null);
  }

  public <T extends SorobanPayload> List<T> readList(Class<T> type) {
    return readList(type, null);
  }

  public <T extends SorobanPayload> List<T> readList(Class<T> type, Predicate<T> filterOrNull) {
    CallbackWithArg<UntypedPayload, T> adapt = untypedPayload -> untypedPayload.read(type);
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

  public int size() {
    return list.size();
  }

  public boolean isEmpty() {
    return list.isEmpty();
  }

  public Optional<? extends UntypedPayload> getFirst() {
    return isEmpty() ? Optional.empty() : Optional.of(list.iterator().next());
  }
}
