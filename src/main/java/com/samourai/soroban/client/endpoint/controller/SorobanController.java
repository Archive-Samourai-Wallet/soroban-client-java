package com.samourai.soroban.client.endpoint.controller;

import com.samourai.soroban.client.endpoint.SorobanEndpoint;
import com.samourai.soroban.client.rpc.RpcSession;
import com.samourai.wallet.util.AbstractOrchestrator;
import com.samourai.wallet.util.AsyncUtil;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public abstract class SorobanController<T, E extends SorobanEndpoint<T, ?, ?>>
    extends AbstractOrchestrator {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  protected final AsyncUtil asyncUtil = AsyncUtil.getInstance();
  protected final RpcSession rpcSession;
  protected final E endpoint;

  protected final String logInfo;
  private int nbProcessed;

  public SorobanController(
      int loopDelayMs, int startDelayMs, String logInfo, RpcSession rpcSession, E endpoint) {
    super(loopDelayMs, startDelayMs, null);
    this.logInfo = logInfo + " sorobanDir=" + endpoint.getDir();
    this.rpcSession = rpcSession;
    this.endpoint = endpoint;
    this.nbProcessed = 0;
  }

  // default loopDelay = endpoint.pollingFrequency
  public SorobanController(int startDelayMs, String logInfo, RpcSession rpcSession, E endpoint) {
    this(endpoint.getPollingFrequencyMs(), startDelayMs, logInfo, rpcSession, endpoint);
  }

  @Override
  protected String getThreadName() {
    return super.getThreadName() + logInfo;
  }

  protected abstract Collection<T> fetch() throws Exception;

  protected void onExpiring(String key) throws Exception {}

  @Override
  protected synchronized void runOrchestrator() {
    MDC.put("mdc", "sorobanDir=" + endpoint.getDir() + ", " + logInfo);
    try {
      Collection<T> messages = fetch();
      for (T message : messages) {
        try {
          onRequest(message);
          nbProcessed++;
        } catch (Exception e) {
          log.error("onRequest failed", e);
        }
      }
    } catch (Exception e) {
      log.error("Failed to fetch", e);
    }
    MDC.clear();
  }

  protected abstract void onRequest(T request) throws Exception;

  protected void remove(T request) throws Exception {
    // delete request
    rpcSession
        .withSorobanClient(sorobanClient -> endpoint.remove(sorobanClient, request))
        .subscribe();
  }

  public int getNbProcessed() {
    return nbProcessed;
  }
}
