package com.samourai.soroban.client.endpoint.meta.typed;

import com.samourai.soroban.client.endpoint.meta.SorobanList;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaType;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanListTyped extends SorobanList<SorobanItemTyped> {
  private static final Logger log = LoggerFactory.getLogger(SorobanListTyped.class);

  public SorobanListTyped(List<SorobanItemTyped> listPayloads) {
    super(listPayloads);
  }

  @Override
  public SorobanListTyped filter(Predicate<SorobanItemTyped> filterOrNull) {
    return (SorobanListTyped) super.filter(filterOrNull);
  }

  @Override
  public SorobanListTyped filterBySender(PaymentCode... senders) {
    return (SorobanListTyped) super.filterBySender(senders);
  }

  public SorobanListTyped filterByType(Class type) {
    return filter(SorobanWrapperMetaType.filterByType(type));
  }

  public <T> SorobanListTyped filterByObject(
      Class<T> type, BiPredicate<SorobanItemTyped, T> filterOrNull) {
    filterByType(type);
    return filter(
        i -> {
          try {
            T object = i.readOn(type);
            if (filterOrNull == null || filterOrNull.test(i, object)) {
              return true;
            }
          } catch (Exception e) {
            log.error("filterByType(): item ignored due to error on " + i.getRawEntry(), e);
          }
          return false;
        });
  }

  public SorobanListTyped filterLatestBySenderAndType(Class type) {
    filterByType(type);
    distinctLatestBySender();
    return this;
  }

  public <T> List<T> getListObjects(Class<T> type) {
    return getListObjects(type, null);
  }

  public <T> List<T> getListObjects(Class<T> type, BiPredicate<SorobanItemTyped, T> filterOrNull) {
    if (filterOrNull != null) {
      filterByObject(type, filterOrNull);
    }
    return listPayloads.stream()
        .map(
            i -> {
              try {
                return i.readOn(type);
              } catch (Exception e) {
                log.error("getListObjects(): item ignored due to error on " + toString(), e);
                return null;
              }
            })
        .filter(i -> i != null)
        .collect(Collectors.toList());
  }

  @Override
  public SorobanListTyped distinctLatestBySender() {
    return (SorobanListTyped) super.distinctLatestBySender();
  }

  @Override
  public SorobanListTyped sortByNonce(boolean desc) {
    return (SorobanListTyped) super.sortByNonce(desc);
  }
}
