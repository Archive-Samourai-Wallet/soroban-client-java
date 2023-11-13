package com.samourai.soroban.client.protocol;

import com.samourai.wallet.bip47.rpc.Bip47Partner;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanProtocol {
  private static final Logger log = LoggerFactory.getLogger(SorobanProtocol.class);

  protected final WhirlpoolNetwork whirlpoolNetwork;
  protected final String protocolId;
  protected final String protocolVersion;

  public SorobanProtocol(
      WhirlpoolNetwork whirlpoolNetwork, String protocolId, String protocolVersion) {
    this.whirlpoolNetwork = whirlpoolNetwork;
    this.protocolId = protocolId;
    this.protocolVersion = protocolVersion;
  }

  protected String hashDir(String dir) {
    return /*Util.sha256Hex(*/ dir /*)*/; // TODO enable hashing
  }

  protected String getDir(String id) {
    return hashDir(getDirClear(id));
  }

  protected String getDirClear(String id) {
    return whirlpoolNetwork.name() + "/" + protocolId + "/" + protocolVersion + "/" + id;
  }

  protected String getDirShared(Bip47Partner bip47Partner, String id) {
    return hashDir(getDirSharedClear(bip47Partner, id));
  }

  protected String getDirSharedClear(Bip47Partner bip47Partner, String id) {
    return getDirClear("SESSION/" + bip47Partner.getSharedAddressBech32() + "/" + id);
  }

  public WhirlpoolNetwork getWhirlpoolNetwork() {
    return whirlpoolNetwork;
  }
}
