package com.samourai.soroban.client.endpoint;

import com.samourai.soroban.client.SorobanClient;
import com.samourai.soroban.client.endpoint.meta.SorobanFilter;
import com.samourai.soroban.client.rpc.RpcSession;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface SorobanEndpoint<I, S, F extends SorobanFilter<I>> {
  Completable send(SorobanClient sorobanClient, S payload);

  Single<I> sendSingle(SorobanClient sorobanClient, S payload);

  Completable remove(SorobanClient sorobanClient, I entry);

  Completable removeRaw(SorobanClient sorobanClient, String rawEntry);

  Single<Optional<I>> findAny(SorobanClient sorobanClient);

  Single<Optional<I>> findAny(SorobanClient sorobanClient, Consumer<F> filterBuilder);

  // loop until value found, at endpoint.polling frequency
  I loopWaitAny(RpcSession rpcSession, long timeoutMs) throws Exception;

  I loopWaitAny(RpcSession rpcSession, long timeoutMs, Consumer<F> filterBuilder) throws Exception;

  Single<List<I>> getList(SorobanClient sorobanClient);

  Single<List<I>> getList(SorobanClient sorobanClient, Consumer<F> filterBuilder);

  SorobanEndpoint getEndpointReply(I request, Bip47Encrypter encrypter);

  void setNoReplay(boolean noReplay);

  int getExpirationMs();

  int getPollingFrequencyMs();

  void setPollingFrequencyMs(int pollingFrequencyMs);

  int getResendFrequencyWhenNoReplyMs();

  void setResendFrequencyWhenNoReplyMs(int resendFrequencyWhenNoReplyMs);

  String getDir();
}
