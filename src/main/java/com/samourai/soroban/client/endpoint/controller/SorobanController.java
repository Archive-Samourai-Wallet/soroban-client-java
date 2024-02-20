package com.samourai.soroban.client.endpoint.controller;

import com.samourai.soroban.client.endpoint.SorobanEndpoint;
import com.samourai.soroban.client.rpc.RpcSession;
import com.samourai.wallet.util.AbstractOrchestrator;
import com.samourai.wallet.util.AsyncUtil;
import com.samourai.wallet.util.Pair;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public abstract class SorobanController<T, E extends SorobanEndpoint<T, ?, ?>>
    extends AbstractOrchestrator {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  protected final AsyncUtil asyncUtil = AsyncUtil.getInstance();
  protected final RpcSession rpcSession;
  protected final E endpoint;
  private long expirationMs; // should be > LOOP_DELAY

  protected final String logInfo;

  private Map<String, Pair<Long, Long>> processedById; // Pair(process time,nonce) by uniqueId

  private int nbMessages = 0;
  private int nbProcesseds = 0;
  private int nbExistings = 0;
  private int nbIgnored = 0;

  public SorobanController(
      int loopDelayMs, int startDelayMs, String logInfo, RpcSession rpcSession, E endpoint) {
    super(loopDelayMs, startDelayMs, null);
    this.logInfo = logInfo + " sorobanDir=" + endpoint.getDir();
    this.rpcSession = rpcSession;
    this.endpoint = endpoint;
    this.expirationMs = endpoint.getExpirationMs() * 3;
    if (log.isDebugEnabled()) {
      log.debug("starting...");
    }
  }

  // default loopDelay = endpoint.pollingFrequency
  public SorobanController(int startDelayMs, String logInfo, RpcSession rpcSession, E endpoint) {
    this(endpoint.getPollingFrequencyMs(), startDelayMs, logInfo, rpcSession, endpoint);
  }

  @Override
  protected String getThreadName() {
    return super.getThreadName() + logInfo;
  }

  protected void setExpirationMs(long expirationMs) {
    this.expirationMs = expirationMs;
  }

  protected abstract Collection<T> fetch() throws Exception;

  protected abstract String computeUniqueId(T request);

  protected abstract Long computeNonce(T request);

  protected abstract void onRequestNew(T request, String key) throws Exception;

  protected void onRequestExisting(T request, String key) throws Exception {}

  protected void onRequestIgnored(T request, String key) throws Exception {}

  protected void onExpiring(String key) throws Exception {}

  @Override
  protected synchronized void runOrchestrator() {
    MDC.put("mdc", "sorobanDir=" + endpoint.getDir() + ", " + logInfo);
    nbProcesseds = 0;
    nbExistings = 0;
    nbIgnored = 0;
    long now = System.currentTimeMillis();
    try {
      Collection<T> messages = fetch();
      nbMessages = messages.size();
      for (T message : messages) {
        try {
          onRequestAny(message, now);
        } catch (Exception e) {
          log.error("onRequestAny failed", e);
        }
      }
    } catch (Exception e) {
      log.error("Failed to fetch", e);
    }

    // clean expired inputs
    int nbExpired = cleanup();
    /*if (nbProcesseds > 0 || nbExpired > 0) {
      logDebug(
          nbProcesseds
              + "/"
              + nbMessages
              + " processeds, "
              + nbExistings
              + " existings, "
              + nbExpired
              + " expired");
    }*/
    MDC.clear();
  }

  // overridable
  protected boolean isRequestIgnored(T message, String uniqueId) {
    return false;
  }

  // overridable
  protected boolean isRequestCacheable(T message) {
    return true;
  }

  protected void onRequestAny(T request, long now) {
    String uniqueId = computeUniqueId(request);
    if (!isRequestIgnored(request, uniqueId)) {
      Long requestNonce = computeNonce(request); // may be null
      boolean requestCacheable = isRequestCacheable(request);
      Pair<Long, Long> existing = processedById.get(uniqueId);
      if (requestCacheable && existing != null) {
        try {
          Long lastNonce = existing.getRight();
          if (lastNonce == null || requestNonce == null || requestNonce > lastNonce) {
            nbExistings++;
            onRequestExisting(request, uniqueId);
          } else {
            nbIgnored++;
            log.warn(
                "REQUEST (repeat) "
                    + uniqueId
                    + " => ignored: requestNonce="
                    + requestNonce
                    + " <= lastNonce="
                    + lastNonce);
            onRequestIgnored(request, uniqueId);
          }
        } catch (Exception e) {
          log.error("Error on existing message:", e);
        }
      } else {
        nbProcesseds++;
        if (requestCacheable) {
          processedById.put(uniqueId, Pair.of(now, requestNonce));
        }
        try {
          // logDebug("Processing: " + uniqueId);
          onRequestNew(request, uniqueId);
        } catch (Exception e) {
          log.error("Error processing a message:", e);
        }
      }
    }
    try {
      remove(request);
    } catch (Exception e) {
      log.error("Error removing a message:", e);
    }
  }

  private synchronized int cleanup() {
    long minProcessTime = System.currentTimeMillis() - expirationMs;
    // cleanup expired
    Set<String> expiredKeys =
        processedById.entrySet().stream()
            .filter(e -> e.getValue().getLeft() < minProcessTime)
            .map(e -> e.getKey())
            .collect(Collectors.toSet()); // required to avoid ConcurrentModificationException
    expiredKeys.forEach(
        key -> {
          try {
            onExpiring(key);
          } catch (Exception e) {
            log.error("Error on expiring message:", e);
          }
          processedById.remove(key);
        });
    return expiredKeys.size();
  }

  @Override
  protected void resetOrchestrator() {
    super.resetOrchestrator();
    processedById = new LinkedHashMap<>();
  }

  // TODO @PreDestroy
  @Override
  public synchronized void stop() {
    super.stop();
    processedById.clear();
  }

  protected void remove(T request) throws Exception {
    // delete request
    rpcSession
        .withSorobanClient(sorobanClient -> endpoint.remove(sorobanClient, request))
        .subscribe();
  }

  protected int getNbMessages() {
    return nbMessages;
  }

  // for tests
  protected int getNbProcesseds() {
    return nbProcesseds;
  }

  // for tests
  protected int getNbExistings() {
    return nbExistings;
  }

  // for tests
  public int getNbIgnored() {
    return nbIgnored;
  }

  // for tests
  protected Map<String, Pair<Long, Long>> getProcessedById() {
    return processedById;
  }
}
