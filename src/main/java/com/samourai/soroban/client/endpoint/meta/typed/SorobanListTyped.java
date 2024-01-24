package com.samourai.soroban.client.endpoint.meta.typed;

import com.samourai.soroban.client.endpoint.meta.SorobanList;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaType;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanListTyped extends SorobanList<SorobanItemTyped> {
  private static final Logger log = LoggerFactory.getLogger(SorobanListTyped.class);

  public SorobanListTyped(List<SorobanItemTyped> listPayloads) {
    super(listPayloads);
  }

  public SorobanListTyped filterByType(Class type) {
    // filter by type
    this.listPayloads =
        listPayloads.stream()
            .filter(SorobanWrapperMetaType.filterByType(type))
            .collect(Collectors.toList());
    return this;
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
    return filterByType(type).getList().stream()
        .map(
            i -> {
              try {
                T object = i.readOn(type);
                if (filterOrNull != null && !filterOrNull.test(i, object)) {
                  return null; // skipped by filter
                }
                return object;
              } catch (Exception e) {
                log.error("getListObjects(): item ignored due to readOn error", e);
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
