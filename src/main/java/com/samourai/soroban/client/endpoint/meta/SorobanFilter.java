package com.samourai.soroban.client.endpoint.meta;

import java.util.stream.Stream;

public interface SorobanFilter<I> {
  Stream<I> applyFilter(Stream<I> stream);
}
