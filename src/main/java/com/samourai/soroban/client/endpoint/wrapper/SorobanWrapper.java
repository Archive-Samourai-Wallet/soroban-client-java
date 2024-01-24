package com.samourai.soroban.client.endpoint.wrapper;

import com.samourai.wallet.bip47.rpc.Bip47Encrypter;

public interface SorobanWrapper<E> {
  E onSend(Bip47Encrypter encrypter, E entry, Object initialPayload) throws Exception;

  E onReceive(Bip47Encrypter encrypter, E entry) throws Exception;
}
