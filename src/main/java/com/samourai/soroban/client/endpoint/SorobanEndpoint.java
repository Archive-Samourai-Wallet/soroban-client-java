package com.samourai.soroban.client.endpoint;

import com.samourai.soroban.client.SorobanClient;
import com.samourai.soroban.client.SorobanPayloadable;
import com.samourai.soroban.client.rpc.RpcSession;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.Optional;

public interface SorobanEndpoint<I, L> {
  Completable send(SorobanClient sorobanClient, String payload) throws Exception;

  Completable send(SorobanClient sorobanClient, SorobanPayloadable sorobanPayloadable)
      throws Exception;

  Completable delete(SorobanClient sorobanClient, I entry) throws Exception;

  Single<Optional<I>> getNext(SorobanClient sorobanClient) throws Exception;

  Single<I> waitNext(RpcSession rpcSession);

  Single<L> getList(SorobanClient sorobanClient) throws Exception;

  String getPathReply(I entry);

  SorobanEndpoint getEndpointReply(I request);

  SorobanApp getApp();

  String computeUniqueId(I entry);
}
