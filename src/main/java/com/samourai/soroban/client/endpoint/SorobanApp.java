package com.samourai.soroban.client.endpoint;

import com.samourai.wallet.bip47.rpc.Bip47Partner;
import com.samourai.wallet.constants.WhirlpoolNetwork;

public class SorobanApp {
  protected final WhirlpoolNetwork whirlpoolNetwork;
  protected final String appId;
  protected final String appVersion;

  public SorobanApp(WhirlpoolNetwork whirlpoolNetwork, String appId, String appVersion) {
    this.whirlpoolNetwork = whirlpoolNetwork;
    this.appId = appId;
    this.appVersion = appVersion;
  }

  public String getDir(String id) {
    return whirlpoolNetwork.name() + "/" + appId + "/" + appVersion + "/" + id;
  }

  public String getDirShared(Bip47Partner bip47Partner, String id) {
    return getDir("SESSION/" + bip47Partner.getSharedAddressBech32() + "/" + id);
  }

  public WhirlpoolNetwork getWhirlpoolNetwork() {
    return whirlpoolNetwork;
  }

  public String getAppId() {
    return appId;
  }

  public String getAppVersion() {
    return appVersion;
  }
}
