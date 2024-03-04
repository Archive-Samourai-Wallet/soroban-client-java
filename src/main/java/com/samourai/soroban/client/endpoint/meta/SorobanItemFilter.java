package com.samourai.soroban.client.endpoint.meta;

import com.samourai.soroban.client.endpoint.meta.typed.SorobanItemTyped;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaNonce;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaSender;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaType;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanItemFilter<I extends SorobanItem> implements SorobanFilter<I> {
  private static final Logger log = LoggerFactory.getLogger(SorobanItemFilter.class);
  protected Predicate<I> filter;
  protected List<Function<Stream<I>, Stream<I>>> transformers;

  public SorobanItemFilter() {
    this.filter = null;
    this.transformers = new LinkedList<>();
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

  public SorobanItemFilter<I> sortByNonce(boolean desc) {
    Comparator<I> comparatorByNonce =
        (Comparator<I>) SorobanWrapperMetaNonce.getComparatorByNonce();
    Comparator<I> comparator = desc ? comparatorByNonce.reversed() : comparatorByNonce;
    transformers.add(stream -> stream.sorted(comparator));
    return this;
  }

  public SorobanItemFilter<I> filterBySenderWithLastNonce(PaymentCode sender) {
    // last nonce
    return sortByNonce(true)
        //  by sender
        .filterBySender(sender);
  }

  public SorobanItemFilter<I> distinctBySenderWithLastNonce() {
    transformers.add(stream -> SorobanWrapperMetaSender.distinctBySenderWithLastNonce(stream));
    return this;
  }

  public SorobanItemFilter<I> distinctByUniqueIdWithLastNonce() {
    BinaryOperator<I> mergeLastByNonce =
        (BinaryOperator<I>) SorobanWrapperMetaNonce.mergeLastByNonce();
    transformers.add(
        stream ->
            stream
                .collect(
                    Collectors.toMap(
                        // distinct by uniqueId
                        p -> p.getUniqueId(),
                        p -> p,
                        // keep last payload with highest nonce
                        mergeLastByNonce))
                .values()
                .stream());
    return this;
  }

  public <T> SorobanItemFilter<I> filterByObject(
      Class<T> type, BiPredicate<SorobanItemTyped, T> filterOrNull) {
    filterByType(type);
    return filter(
        i -> {
          try {
            SorobanItemTyped itemTyped = (SorobanItemTyped) i; // TODO cast
            T object = itemTyped.read(type);
            if (filterOrNull == null || filterOrNull.test(itemTyped, object)) {
              return true;
            }
          } catch (Exception e) {
            log.error("filterByType(): item ignored due to error on " + i.getRawEntry(), e);
          }
          return false;
        });
  }

  public SorobanItemFilter<I> filter(Predicate<I> filterOrNull) {
    if (filter == null) {
      filter = i -> true;
    }
    filter = filter.and(sorobanItem -> filterOrNull.test(sorobanItem));
    return this;
  }

  public SorobanItemFilter<I> filterBySender(PaymentCode... senders) {
    filter(SorobanWrapperMetaSender.filterBySender(senders));
    return this;
  }

  public SorobanItemFilter<I> filterByType(Class... types) {
    filter(SorobanWrapperMetaType.filterByType(types));
    return this;
  }

  @Override
  public Stream<I> applyFilter(Stream<I> stream) {
    // apply filters
    if (filter != null) {
      stream =
          stream
              /*.map(
              i -> {
                log.debug("BEFORE_FILTER " + i.getMetaSender() + " " + i.getMetaNonce());
                return i;
              })*/
              .filter(filter);
    }

    // apply transformers
    if (!transformers.isEmpty()) {
      for (Function<Stream<I>, Stream<I>> transformer : transformers) {
        stream = transformer.apply(stream);
      }
    }
    /*stream =
    stream.map(
        i -> {
          log.debug("AFTER_FILTER " + i.getMetaSender() + " " + i.getMetaNonce());
          return i;
        });*/
    return stream;
  }
}
