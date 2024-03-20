package com.samourai.soroban.client;

import com.samourai.soroban.client.rpc.RpcClientService;
import com.samourai.soroban.client.wallet.SorobanWalletService;
import com.samourai.wallet.util.ExtLibJConfig;

public class SorobanConfig {
  private ExtLibJConfig extLibJConfig;
  private RpcClientService rpcClientService;
  private SorobanWalletService sorobanWalletService;

  public SorobanConfig(
      ExtLibJConfig extLibJConfig,
      RpcClientService rpcClientService,
      SorobanWalletService sorobanWalletService) {
    this.extLibJConfig = extLibJConfig;
    this.rpcClientService = rpcClientService;
    this.sorobanWalletService = sorobanWalletService;
  }

  public SorobanConfig(ExtLibJConfig extLibJConfig) {
    this.extLibJConfig = extLibJConfig;
    this.rpcClientService = new RpcClientService(extLibJConfig);
    this.sorobanWalletService = new SorobanWalletService(extLibJConfig, rpcClientService);
  }

  public ExtLibJConfig getExtLibJConfig() {
    return extLibJConfig;
  }

  public RpcClientService getRpcClientService() {
    return rpcClientService;
  }

  public SorobanWalletService getSorobanWalletService() {
    return sorobanWalletService;
  }
}
