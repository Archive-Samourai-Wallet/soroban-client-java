package com.samourai.soroban.client.rpc;

import com.samourai.wallet.util.urlStatus.UpStatusPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanUpStatusPool extends UpStatusPool {
  private static final Logger log = LoggerFactory.getLogger(SorobanUpStatusPool.class);
  private static final int DOWN_EXPIRATION_DELAY_MS = 600000; // 10min

  public SorobanUpStatusPool() {
    super(DOWN_EXPIRATION_DELAY_MS);
  }
}
