package com.samourai.soroban.client.rpc;

import com.samourai.soroban.client.AbstractTest;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactoryGeneric;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.bitcoinj.core.ECKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcClientTest extends AbstractTest {
  private static final Logger log = LoggerFactory.getLogger(RpcClientTest.class);
  private static final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(5);

  private static final String SIGNATURE_SEED_WORDS =
      "income wisdom battle label wolf confirm shoulder tumble ecology current news taste";
  private static final String SIGNATURE_SEED_PASSPHRASE = "Test@K3y";

  private RpcClient rpcClient;
  private RpcClient rpcClientAuth;

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();
    this.rpcClient = rpcClientService.createRpcSession().withRpcClient(rpcClient -> rpcClient);

    HD_Wallet hdw =
        HD_WalletFactoryGeneric.getInstance()
            .restoreWallet(SIGNATURE_SEED_WORDS, SIGNATURE_SEED_PASSPHRASE, params);
    ECKey authKey = hdw.getAddressAt(0, 0, 0).getECKey();
    this.rpcClientAuth = rpcClientService.createRpcSession().withRpcClient(rpcClient -> rpcClient);
    this.rpcClientAuth.setAuthenticationKey(authKey);
  }

  @Test
  public void testKey() throws Exception {
    String key = "com.samourai.whirlpool.ro.coordinators.TESTNET";

    Map<String, Integer> messagesByServer = new LinkedHashMap<>();
    for (String serverUrl : initialSorobanServerTestnetClearUrls) {
      try {
        synchronized (this) {
          try {
            wait(100);
          } catch (InterruptedException e) {
          }
        }
        int nbValues =
            asyncUtil.blockingGet(
                    rpcClientService
                        .createRpcClient(serverUrl + RpcClient.ENDPOINT_RPC)
                        .directoryValues(key))
                .length;
        messagesByServer.put(serverUrl, nbValues);
      } catch (Exception e) {
        messagesByServer.put(serverUrl, -1);
        log.error("", e);
      }
    }
    System.out.println("RESULT=" + messagesByServer);

    for (Map.Entry<String, Integer> entry : messagesByServer.entrySet()) {
      String url = entry.getKey().replace("http://", "").replace(":", " ");
      if (entry.getValue() == -1) {
        System.out.println("DexServer is down: " + url);
      }
    }
    for (Map.Entry<String, Integer> entry : messagesByServer.entrySet()) {
      String url = entry.getKey().replace("http://", "").replace(":", " ");
      if (entry.getValue() == 0) {
        System.out.println("DexServer is desynchronized: " + url + ": 0 message");
      }
    }
    for (Map.Entry<String, Integer> entry : messagesByServer.entrySet()) {
      String url = entry.getKey().replace("http://", "").replace(":", " ");
      if (entry.getValue() > 0) {
        System.out.println("DexServer is OK: " + url + ": " + entry.getValue() + " messages");
      }
    }
  }

  @Test
  public void directoryValue_sucess() throws Exception {
    String key = "key";
    String value = "valueOne";

    // cleanup
    asyncUtil.blockingAwait(rpcClient.directoryRemove(key, value));

    asyncUtil.blockingAwait(rpcClient.directoryAdd(key, value, RpcMode.NORMAL));
    Assertions.assertEquals(value, asyncUtil.blockingGet(rpcClient.directoryValue(key)));
  }

  @Test
  public void directoryValues_sucess() throws Exception {
    String key = "key";
    String value = "valueOne";

    // cleanup
    asyncUtil.blockingAwait(rpcClient.directoryRemove(key, value));

    asyncUtil.blockingAwait(rpcClient.directoryAdd(key, value, RpcMode.NORMAL));
    Assertions.assertEquals(value, asyncUtil.blockingGet(rpcClient.directoryValues(key))[0]);
  }

  @Test
  public void directoryValue_failure() throws Exception {
    String key = "key";
    String value = "valueOne";

    // cleanup
    asyncUtil.blockingAwait(rpcClient.directoryRemove(key, value));

    try {
      asyncUtil.blockingGet(rpcClient.directoryValue(key));
      Assertions.assertTrue(false);
    } catch (NoValueRpcException e) {
      // ok
    }
  }

  @Test
  public void read_on_writeOnly_deny() throws Exception {
    String key = "com.samourai.whirlpool.wo";
    String value = "valueOne";

    // allow cleanup
    asyncUtil.blockingAwait(rpcClient.directoryRemove(key, value));

    // allow writing
    asyncUtil.blockingAwait(rpcClient.directoryAdd(key, value, RpcMode.NORMAL));

    // deny reading
    Assertions.assertThrows(
        PermissionDeniedRpcException.class,
        () -> asyncUtil.blockingGet(rpcClient.directoryValues(key)));
  }

  @Test
  public void read_on_writeOnly_allow() throws Exception {
    String key = "com.samourai.whirlpool.wo";
    String value = "valueOne";

    // allow cleanup
    asyncUtil.blockingAwait(rpcClientAuth.directoryRemove(key, value));

    // allow writing
    asyncUtil.blockingAwait(rpcClientAuth.directoryAdd(key, value, RpcMode.NORMAL));

    // allow reading with signature
    Assertions.assertEquals(1, asyncUtil.blockingGet(rpcClientAuth.directoryValues(key)).length);
  }

  @Test
  public void write_on_readOnly_deny() throws Exception {
    String key = "com.samourai.whirlpool.ro";
    String value = "valueOne";

    int nbValues = asyncUtil.blockingGet(rpcClient.directoryValues(key)).length;

    // deny remove
    Assertions.assertThrows(
        IOException.class, () -> asyncUtil.blockingAwait(rpcClient.directoryRemove(key, value)));

    // deny writing
    Assertions.assertThrows(
        IOException.class,
        () -> asyncUtil.blockingAwait(rpcClient.directoryAdd(key, value, RpcMode.NORMAL)));

    // allow reading
    Assertions.assertEquals(nbValues, asyncUtil.blockingGet(rpcClient.directoryValues(key)).length);
  }

  @Test
  public void write_on_readOnly_allow() throws Exception {
    String key = "com.samourai.whirlpool.ro";
    String value = "valueOne";

    // allow remove with signature
    asyncUtil.blockingAwait(rpcClientAuth.directoryRemove(key, value));
    Assertions.assertTrue(asyncUtil.blockingGet(rpcClientAuth.directoryValues(key)).length == 0);

    // allow writing with signature
    asyncUtil.blockingAwait(rpcClientAuth.directoryAdd(key, value, RpcMode.NORMAL));

    // allow reading
    Assertions.assertTrue(asyncUtil.blockingGet(rpcClientAuth.directoryValues(key)).length == 1);
  }

  @Test
  public void directoryValueWait_success() throws Exception {
    String key = "key";
    String value = "valueOne";

    // cleanup
    asyncUtil.blockingAwait(rpcClient.directoryRemove(key, value));

    // add after 2 seconds
    executor.schedule(
        () -> {
          try {
            log.info("directoryAdd");
            asyncUtil.blockingAwait(rpcClient.directoryAdd(key, value, RpcMode.NORMAL));
          } catch (Exception e) {
            setException(e);
          }
        },
        1000,
        TimeUnit.MILLISECONDS);

    // wait immediately
    log.info("wait...");
    Assertions.assertEquals(
        value,
        asyncUtil.blockingGet(rpcClient.directoryValueWait(key, TIMEOUT_MS, RETRY_DELAY_MS)));
  }

  @Test
  public void directoryValueWait_fail() throws Exception {
    String key = "key";
    String value = "valueOne";

    // cleanup
    asyncUtil.blockingAwait(rpcClient.directoryRemove(key, value));

    // add too late
    executor.schedule(
        () -> {
          try {
            log.info("directoryAdd");
            asyncUtil.blockingAwait(rpcClient.directoryAdd(key, value, RpcMode.NORMAL));
          } catch (Exception e) {
            setException(e);
          }
        },
        2000,
        TimeUnit.MILLISECONDS);

    // wait immediately
    log.info("wait...");
    try {
      asyncUtil.blockingGet(rpcClient.directoryValueWait(key, TIMEOUT_MS, RETRY_DELAY_MS));
      Assertions.assertTrue(false);
    } catch (TimeoutException e) {
      // ok
    }
  }

  @Test
  public void directoryValueWaitAndRemove() throws Exception {
    String key = "key";
    String value = "valueOne";

    // cleanup
    asyncUtil.blockingAwait(rpcClient.directoryRemove(key, value));

    // add after 2 seconds
    executor.schedule(
        () -> {
          try {
            log.info("directoryAdd");
            asyncUtil.blockingAwait(rpcClient.directoryAdd(key, value, RpcMode.NORMAL));
          } catch (Exception e) {
            setException(e);
          }
        },
        1000,
        TimeUnit.MILLISECONDS);

    // wait immediately
    log.info("waitAndRemove...");
    Assertions.assertEquals(
        value,
        asyncUtil.blockingGet(
            rpcClient.directoryValueWaitAndRemove(key, TIMEOUT_MS, RETRY_DELAY_MS)));

    // verify it has been removed
    try {
      Thread.sleep(300);
      asyncUtil.blockingGet(rpcClient.directoryValue(key));
      Assertions.assertTrue(false);
    } catch (NoValueRpcException e) {
      // ok
    }
  }
}
