package com.samourai.soroban.client.endpoint.wrapper;

import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import com.samourai.wallet.util.Pair;

public interface SorobanWrapper<M> {
  Pair<String, M> onSend(Bip47Encrypter encrypter, Pair<String, M> entry, Object initialPayload)
      throws Exception;

  Pair<String, M> onReceive(Bip47Encrypter encrypter, Pair<String, M> entry) throws Exception;
}
