package com.samourai.soroban.client.endpoint.controller;

import com.samourai.soroban.client.SorobanPayloadable;
import com.samourai.soroban.client.endpoint.SorobanEndpoint;
import com.samourai.soroban.client.rpc.RpcSession;
import com.samourai.wallet.util.AbstractOrchestrator;
import com.samourai.wallet.util.AsyncUtil;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SorobanController<T, E extends SorobanEndpoint<T, ?>>
    extends AbstractOrchestrator {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  protected static final int LOOP_DELAY_SLOW = 15000; // 15s
  protected static final int LOOP_DELAY_FAST = 2000; // 2s

  protected final AsyncUtil asyncUtil = AsyncUtil.getInstance();
  protected final RpcSession rpcSession;
  protected final E endpoint;
  private long expirationMs = 1800000; // 30m, should be > LOOP_DELAY

  protected final String logId;

  private Map<String, Long> processedById; // process time by uniqueId

  private int nbMessages = 0;
  private int nbProcesseds = 0;
  private int nbExistings = 0;

  public SorobanController(int LOOP_DELAY, String logId, RpcSession rpcSession, E endpoint) {
    super(LOOP_DELAY, 0, null);
    this.logId = logId;
    this.rpcSession = rpcSession;
    this.endpoint = endpoint;
    logDebug("starting...");
  }

  @Override
  protected String getThreadName() {
    return super.getThreadName() + logId;
  }

  protected void setExpirationMs(long expirationMs) {
    this.expirationMs = expirationMs;
  }

  protected void logDebug(String msg) {
    if (log.isDebugEnabled()) {
      log.debug("[" + logId + "] " + msg);
    }
  }

  protected abstract Collection<T> fetch() throws Exception;

  protected abstract void process(T message, String key) throws Exception;

  protected abstract String computeUniqueId(T message);

  protected void onExisting(T message, String key) throws Exception {}

  protected void onExpiring(String key) throws Exception {}

  @Override
  protected synchronized void runOrchestrator() {
    nbProcesseds = 0;
    nbExistings = 0;
    long now = System.currentTimeMillis();
    try {
      Collection<T> messages = fetch();
      nbMessages = messages.size();
      for (T message : messages) {
        try {
          onMessage(message, now);
        } catch (Exception e) {
          log.error("", e);
        }
      }
    } catch (Exception e) {
      log.error("[" + logId + "] Failed to fetch from Soroban", e);
    }

    // clean expired inputs
    int nbExpired = cleanup();
    if (nbProcesseds > 0 || nbExpired > 0) {
      logDebug(
          nbProcesseds
              + "/"
              + nbMessages
              + " processeds, "
              + nbExistings
              + " existings, "
              + nbExpired
              + " expired");
    }
  }

  protected void onMessage(T message, long now) throws Exception {
    String uniqueId = computeUniqueId(message);
    if (this.processedById.containsKey(uniqueId)) {
      nbExistings++;
      try {
        onExisting(message, uniqueId);
        delete(message);
      } catch (Exception e) {
        log.error("[" + logId + "] Error on existing message:", e);
      }
    } else {
      nbProcesseds++;
      processedById.put(uniqueId, now);
      try {
        logDebug("Processing: " + uniqueId);
        process(message, uniqueId);
        delete(message);
      } catch (Exception e) {
        log.error("[" + logId + "] Error processing a message:", e);
      }
    }
  }

  private synchronized int cleanup() {
    long minProcessTime = System.currentTimeMillis() - expirationMs;
    // cleanup expired
    Set<String> expiredKeys =
        processedById.entrySet().stream()
            .filter(e -> e.getValue() < minProcessTime)
            .map(e -> e.getKey())
            .collect(Collectors.toSet()); // required to avoid ConcurrentModificationException
    expiredKeys.forEach(
        key -> {
          try {
            onExpiring(key);
          } catch (Exception e) {
            log.error("[" + logId + "] Error on expiring message:", e);
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
    logDebug("stop");
    processedById.clear();
  }

  protected void sendReply(T request, SorobanPayloadable response) throws Exception {
    // reply to request
    rpcSession
        .withSorobanClient(
            sorobanClient -> endpoint.getEndpointReply(request).send(sorobanClient, response))
        .subscribe();
  }

  protected void delete(T request) throws Exception {
    // delete request
    rpcSession
        .withSorobanClient(sorobanClient -> endpoint.delete(sorobanClient, request))
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
  protected Map<String, Long> getProcessedById() {
    return processedById;
  }
}
