package com.samourai.soroban.client.endpoint;

import com.samourai.soroban.client.SorobanConfig;
import com.samourai.wallet.bip47.rpc.Bip47Partner;
import com.samourai.wallet.constants.SamouraiNetwork;

public class SorobanApp {
  protected final SorobanConfig sorobanConfig;
  protected final String appId;
  protected final String appVersion;

  public SorobanApp(SorobanConfig sorobanConfig, String appId, String appVersion) {
    this.sorobanConfig = sorobanConfig;
    this.appId = appId;
    this.appVersion = appVersion;
  }

  public String getDir(String id) {
    SamouraiNetwork samouraiNetwork = sorobanConfig.getExtLibJConfig().getSamouraiNetwork();
    return samouraiNetwork.name() + "/" + appId + "/" + appVersion + "/" + id;
  }

  public String getDirShared(Bip47Partner bip47Partner, String id) {
    return getDir("SESSION/" + bip47Partner.getSharedAddressBech32() + "/" + id);
  }

  public SorobanConfig getSorobanConfig() {
    return sorobanConfig;
  }

  public String getAppId() {
    return appId;
  }

  public String getAppVersion() {
    return appVersion;
  }
}
