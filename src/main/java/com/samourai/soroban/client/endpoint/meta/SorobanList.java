package com.samourai.soroban.client.endpoint.meta;

import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaNonce;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaSender;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanList<I extends SorobanItem> implements List<I> {
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

  public SorobanList<I> filter(Predicate<I> filterOrNull) {
    this.listPayloads =
        listPayloads.stream()
            .filter(sorobanItem -> filterOrNull.test(sorobanItem))
            .collect(Collectors.toList());
    return this;
  }

  public SorobanList<I> filterBySender(PaymentCode... senders) {
    return filter(SorobanWrapperMetaSender.filterBySender(senders));
  }

  public Optional<I> getLastNonceBySender(PaymentCode sender) {
    sortByNonce(true);
    return this.listPayloads.stream().filter(i -> sender.equals(i.getMetaSender())).findFirst();
  }

  public Optional<I> findFirst() {
    return isEmpty() ? Optional.empty() : Optional.of(listPayloads.iterator().next());
  }

  public Optional<I> findLast() {
    return isEmpty() ? Optional.empty() : Optional.of(listPayloads.get(listPayloads.size() - 1));
  }

  // LIST

  @Override
  public int size() {
    return listPayloads.size();
  }

  @Override
  public boolean isEmpty() {
    return listPayloads.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return listPayloads.contains(o);
  }

  @NotNull
  @Override
  public Iterator<I> iterator() {
    return listPayloads.iterator();
  }

  @NotNull
  @Override
  public Object[] toArray() {
    return listPayloads.toArray();
  }

  @NotNull
  @Override
  public <T> T[] toArray(@NotNull T[] a) {
    return listPayloads.toArray(a);
  }

  @Override
  public boolean add(I i) {
    return listPayloads.add(i);
  }

  @Override
  public boolean remove(Object o) {
    return listPayloads.remove(o);
  }

  @Override
  public boolean containsAll(@NotNull Collection<?> c) {
    return listPayloads.containsAll(c);
  }

  @Override
  public boolean addAll(@NotNull Collection<? extends I> c) {
    return listPayloads.addAll(c);
  }

  @Override
  public boolean addAll(int index, @NotNull Collection<? extends I> c) {
    return listPayloads.addAll(c);
  }

  @Override
  public boolean removeAll(@NotNull Collection<?> c) {
    return listPayloads.removeAll(c);
  }

  @Override
  public boolean retainAll(@NotNull Collection<?> c) {
    return listPayloads.retainAll(c);
  }

  @Override
  public void clear() {
    listPayloads.clear();
  }

  @Override
  public I get(int index) {
    return listPayloads.get(index);
  }

  @Override
  public I set(int index, I element) {
    return listPayloads.set(index, element);
  }

  @Override
  public void add(int index, I element) {
    listPayloads.add(index, element);
  }

  @Override
  public I remove(int index) {
    return listPayloads.remove(index);
  }

  @Override
  public int indexOf(Object o) {
    return listPayloads.indexOf(o);
  }

  @Override
  public int lastIndexOf(Object o) {
    return listPayloads.lastIndexOf(o);
  }

  @NotNull
  @Override
  public ListIterator<I> listIterator() {
    return listPayloads.listIterator();
  }

  @NotNull
  @Override
  public ListIterator<I> listIterator(int index) {
    return listPayloads.listIterator(index);
  }

  @NotNull
  @Override
  public List<I> subList(int fromIndex, int toIndex) {
    return listPayloads.subList(fromIndex, toIndex);
  }
}
