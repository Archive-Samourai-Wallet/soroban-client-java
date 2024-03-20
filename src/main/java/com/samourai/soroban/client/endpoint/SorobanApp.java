package com.samourai.soroban.client.endpoint;

import com.samourai.wallet.bip47.rpc.Bip47Partner;
import com.samourai.wallet.constants.SamouraiNetwork;

public class SorobanApp {
  protected final SamouraiNetwork samouraiNetwork;
  protected final String appId;
  protected final String appVersion;

  public SorobanApp(SamouraiNetwork samouraiNetwork, String appId, String appVersion) {
    this.samouraiNetwork = samouraiNetwork;
    this.appId = appId;
    this.appVersion = appVersion;
  }

  public String getDir(String id) {
    return samouraiNetwork.name() + "/" + appId + "/" + appVersion + "/" + id;
  }

  public String getDirShared(Bip47Partner bip47Partner, String id) {
    return getDir("SESSION/" + bip47Partner.getSharedAddressBech32() + "/" + id);
  }

  public SamouraiNetwork getSamouraiNetwork() {
    return samouraiNetwork;
  }

  public String getAppId() {
    return appId;
  }

  public String getAppVersion() {
    return appVersion;
  }
}
