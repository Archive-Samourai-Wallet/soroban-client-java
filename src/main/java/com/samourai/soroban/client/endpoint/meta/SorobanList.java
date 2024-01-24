package com.samourai.soroban.client.endpoint.meta;

import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaNonce;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaSender;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanList<I extends SorobanItem> {
  private static final Logger log = LoggerFactory.getLogger(SorobanList.class);
  protected List<I> listPayloads;

  public SorobanList(List<I> listPayloads) {
    this.listPayloads = listPayloads;
  }

  public SorobanList<I> distinctLatestBySender() {
    this.listPayloads = SorobanWrapperMetaSender.distinctLatestBySender(listPayloads.stream());
    return this;
  }

  /*
  public static <I extends SorobanItem> List<I> distinctLatestById(
      Stream<I> payloads, Function<I, String> getUniqueId) {
    BinaryOperator<I> mergeLastByNonce =
        (BinaryOperator<I>) SorobanWrapperMetaNonce.mergeLastByNonce();
    return new LinkedList<>(
        payloads
            .collect(
                Collectors.toMap(
                    // distinct by ID
                    getUniqueId,
                    p -> p,
                    // keep last payload with highest nonce
                    mergeLastByNonce))
            .values());
  }*/

  public SorobanList<I> sortByNonce(boolean desc) {
    Comparator<I> comparator = (Comparator<I>) SorobanWrapperMetaNonce.getComparatorByNonce();
    if (desc) {
      comparator = comparator.reversed();
    }
    this.listPayloads.sort(comparator);
    return this;
  }

  public List<I> getList() {
    return getList(null);
  }

  public List<I> getList(Predicate<I> filterOrNull) {
    if (filterOrNull == null) {
      return listPayloads;
    }
    return listPayloads.stream()
        .filter(sorobanItem -> filterOrNull.test(sorobanItem))
        .collect(Collectors.toList());
  }

  public Optional<I> getFirst() {
    if (listPayloads.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(listPayloads.iterator().next());
  }

  public Optional<I> getLastBySender(PaymentCode sender) {
    sortByNonce(true);
    return this.listPayloads.stream().filter(i -> sender.equals(i.getMetaSender())).findFirst();
  }

  public int size() {
    return listPayloads.size();
  }

  public boolean isEmpty() {
    return listPayloads.isEmpty();
  }

  /*public Optional<? extends SorobanPayloadTyped> getFirst() {
    return isEmpty() ? Optional.empty() : Optional.of(listPayloads.iterator().next());
  }*/
}
