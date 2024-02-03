package com.samourai.soroban.client.endpoint;

import com.samourai.soroban.client.SorobanClient;
import com.samourai.soroban.client.rpc.RpcSession;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.List;
import java.util.Optional;

public interface SorobanEndpoint<I, L extends List<I>, S> {
  Completable send(SorobanClient sorobanClient, S payload) throws Exception;

  Single<I> sendSingle(SorobanClient sorobanClient, S payload) throws Exception;

  Completable remove(SorobanClient sorobanClient, I entry) throws Exception;

  Completable removeRaw(SorobanClient sorobanClient, String rawEntry) throws Exception;

  Single<Optional<I>> getFirst(SorobanClient sorobanClient) throws Exception;

  Single<Optional<I>> getLast(SorobanClient sorobanClient) throws Exception;

  Single<I> waitNext(RpcSession rpcSession);

  Single<L> getList(SorobanClient sorobanClient) throws Exception;

  String getPathReply(I entry);

  SorobanEndpoint getEndpointReply(I request);

  SorobanApp getApp();

  String computeUniqueId(I entry);

  void setAutoRemove(boolean autoRemove);
}
