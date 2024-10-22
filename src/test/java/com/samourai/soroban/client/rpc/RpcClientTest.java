package com.samourai.soroban.client.rpc;

import com.samourai.soroban.client.AbstractTest;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactoryGeneric;
import com.samourai.wallet.httpClient.HttpUsage;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bitcoinj.core.ECKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcClientTest extends AbstractTest {
  private static final Logger log = LoggerFactory.getLogger(RpcClientTest.class);

  private static final String SIGNATURE_SEED_WORDS =
      "income wisdom battle label wolf confirm shoulder tumble ecology current news taste";
  private static final String SIGNATURE_SEED_PASSPHRASE = "Test@K3y";

  private RpcClient rpcClient;
  private RpcClient rpcClientAuth;

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();
    this.rpcClient = rpcSessionInitiator.withRpcClient(rc -> rc);
    this.rpcClient.setAuthenticationKey(null);

    HD_Wallet hdw =
        HD_WalletFactoryGeneric.getInstance()
            .restoreWallet(SIGNATURE_SEED_WORDS, SIGNATURE_SEED_PASSPHRASE, params);
    ECKey authKey = hdw.getAddressAt(0, 0, 0).getECKey();
    this.rpcClientAuth = rpcSessionInitiator.withRpcClient(rc -> rc);
    this.rpcClientAuth.setAuthenticationKey(authKey);
  }

  @Test
  public void testKey() throws Exception {
    String key = "com.samourai.whirlpool.ro.coordinators.TESTNET";

    Map<String, Integer> messagesByServer = new LinkedHashMap<>();
    for (String sorobanUrl : initialSorobanServerTestnetClearUrls) {
      try {
        synchronized (this) {
          try {
            wait(100);
          } catch (InterruptedException e) {
          }
        }
        int nbValues =
            asyncUtil.blockingGet(
                    sorobanConfig
                        .getRpcClientService()
                        .createRpcClient(sorobanUrl + RpcClient.ENDPOINT_RPC, HttpUsage.SOROBAN)
                        .directoryValues(key))
                .length;
        messagesByServer.put(sorobanUrl, nbValues);
      } catch (Exception e) {
        messagesByServer.put(sorobanUrl, -1);
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
  public void directoryValue_success() throws Exception {
    String key = "directoryValue_success";
    String value = "valueOne";

    // cleanup
    asyncUtil.blockingAwait(rpcClient.directoryRemove(key, value));

    asyncUtil.blockingAwait(rpcClient.directoryAdd(key, value, RpcMode.NORMAL));
    Assertions.assertEquals(value, asyncUtil.blockingGet(rpcClient.directoryValue(key)));
  }

  @Test
  public void directoryValues_success() throws Exception {
    String key = "directoryValues_success";
    String value = "valueOne";

    // cleanup
    asyncUtil.blockingAwait(rpcClient.directoryRemove(key, value));

    asyncUtil.blockingAwait(rpcClient.directoryAdd(key, value, RpcMode.NORMAL));
    Assertions.assertEquals(value, asyncUtil.blockingGet(rpcClient.directoryValues(key))[0]);
  }

  @Test
  public void directoryValues_success_multinodes() throws Exception {
    String key = "directoryValues_success";
    String value = "valueOne";

    // cleanup
    asyncUtil.blockingAwait(rpcClient.directoryRemove(key, value));

    asyncUtil.blockingAwait(rpcClient.directoryAdd(key, value, RpcMode.NORMAL));
    waitSorobanDelay();
    waitSorobanDelay();
    waitSorobanDelay();
    Assertions.assertEquals(value, asyncUtil.blockingGet(rpcClient.directoryValues(key))[0]);
    Assertions.assertEquals(
        value,
        asyncUtil
            .blockingGet(
                rpcSessionInitiator.withRpcClient(rpcClient -> rpcClient.directoryValues(key)))[0]);
    Assertions.assertEquals(
        value,
        asyncUtil
            .blockingGet(
                rpcSessionInitiator.withRpcClient(rpcClient -> rpcClient.directoryValues(key)))[0]);
  }

  @Test
  public void directoryValue_failure() throws Exception {
    String key = "directoryValue_failure";
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

  /* // TODO move to RpcSessionTest
  @Test
  public void directoryValueWait_success() throws Exception {
    String key = "directoryValueWait_success";
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
    Assertions.assertEquals(value, asyncUtil.blockingGet(rpcClient.directoryValueWait(key, 2000)));
  }

  @Test
  public void directoryValueWait_fail() throws Exception {
    String key = "directoryValueWait_fail";
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
      asyncUtil.blockingGet(rpcClient.directoryValueWait(key, 1000));
      Assertions.assertTrue(false);
    } catch (TimeoutException e) {
      // ok
    }
  }

  @Test
  public void directoryValueWaitAndRemove() throws Exception {
    String key = "directoryValueWaitAndRemove";
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
        value, asyncUtil.blockingGet(rpcClient.directoryValueWaitAndRemove(key, 2000)));

    // verify it has been removed
    try {
      Thread.sleep(300);
      asyncUtil.blockingGet(rpcClient.directoryValue(key));
      Assertions.assertTrue(false);
    } catch (NoValueRpcException e) {
      // ok
    }
  }*/

  //
  // PERMISSIONS
  //

  /*@Test
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
  }*/
}
