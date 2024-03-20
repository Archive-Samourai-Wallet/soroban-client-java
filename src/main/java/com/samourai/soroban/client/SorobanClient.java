package com.samourai.soroban.client;

import com.samourai.soroban.client.rpc.RpcClient;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanClient {
  private static final Logger log = LoggerFactory.getLogger(SorobanClient.class.getName());

  private final RpcClient rpcClient;
  private Bip47Encrypter encrypter;

  public SorobanClient(RpcClient rpcClient, Bip47Encrypter encrypter) {
    this.rpcClient = rpcClient;
    this.encrypter = encrypter;
  }

  //

  public RpcClient getRpcClient() {
    return rpcClient;
  }

  public Bip47Encrypter getEncrypter() {
    return encrypter;
  }

  public NetworkParameters getParams() {
    return rpcClient.getParams();
  }
}
