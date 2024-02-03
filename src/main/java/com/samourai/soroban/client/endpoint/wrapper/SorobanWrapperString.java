package com.samourai.soroban.client.endpoint.wrapper;

import com.samourai.wallet.bip47.rpc.Bip47Encrypter;

public interface SorobanWrapperString {

  String onSend(Bip47Encrypter encrypter, String entry, Object initialPayload) throws Exception;

  String onReceive(Bip47Encrypter encrypter, String entry) throws Exception;
}
