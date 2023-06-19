package com.samourai.soroban.client.rpc;

import com.samourai.soroban.client.AbstractTest;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactoryGeneric;
import java.io.IOException;
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
      "all all all all all all all all all all all all";
  private static final String SIGNATURE_SEED_PASSPHRASE = "test";

  private RpcClient rpcClient;

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();
    this.rpcClient = rpcClientService.getRpcClient("test");
  }

  @Test
  public void directoryValue_sucess() throws Exception {
    String key = "key";
    String value = "valueOne";

    // cleanup
    asyncUtil.blockingGet(rpcClient.directoryRemove(key, value));

    asyncUtil.blockingGet(rpcClient.directoryAdd(key, value, RpcMode.normal.name()));
    Assertions.assertEquals(value, asyncUtil.blockingGet(rpcClient.directoryValue(key)));
  }

  @Test
  public void directoryValues_sucess() throws Exception {
    String key = "key";
    String value = "valueOne";

    // cleanup
    asyncUtil.blockingGet(rpcClient.directoryRemove(key, value));

    asyncUtil.blockingGet(rpcClient.directoryAdd(key, value, RpcMode.normal.name()));
    Assertions.assertEquals(value, asyncUtil.blockingGet(rpcClient.directoryValues(key))[0]);
  }

  @Test
  public void directoryValue_failure() throws Exception {
    String key = "key";
    String value = "valueOne";

    // cleanup
    asyncUtil.blockingGet(rpcClient.directoryRemove(key, value));

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
    asyncUtil.blockingGet(rpcClient.directoryRemove(key, value));

    // allow writing
    asyncUtil.blockingGet(rpcClient.directoryAdd(key, value, RpcMode.normal.name()));

    // deny reading
    Assertions.assertThrows(
        PermissionDeniedRpcException.class,
        () -> asyncUtil.blockingGet(rpcClient.directoryValues(key)));
  }

  private void setAuthentication() throws Exception {
    HD_Wallet hdw =
        HD_WalletFactoryGeneric.getInstance()
            .restoreWallet(SIGNATURE_SEED_WORDS, SIGNATURE_SEED_PASSPHRASE, params);
    ECKey signatureKey = hdw.getAddressAt(0, 0, 0).getECKey();
    rpcClient.setAuthentication(signatureKey, params);
  }

  @Test
  public void read_on_writeOnly_allow() throws Exception {
    String key = "com.samourai.whirlpool.wo";
    String value = "valueOne";

    // enable signature
    setAuthentication();

    // allow cleanup
    asyncUtil.blockingGet(rpcClient.directoryRemove(key, value));

    // allow writing
    asyncUtil.blockingGet(rpcClient.directoryAdd(key, value, RpcMode.normal.name()));

    // allow reading with signature
    Assertions.assertEquals(1, asyncUtil.blockingGet(rpcClient.directoryValues(key)).length);
  }

  @Test
  public void write_on_readOnly_deny() throws Exception {
    String key = "com.samourai.whirlpool.ro";
    String value = "valueOne";

    int nbValues = asyncUtil.blockingGet(rpcClient.directoryValues(key)).length;

    // deny remove
    Assertions.assertThrows(
        IOException.class, () -> asyncUtil.blockingGet(rpcClient.directoryRemove(key, value)));

    // deny writing
    Assertions.assertThrows(
        IOException.class,
        () -> asyncUtil.blockingGet(rpcClient.directoryAdd(key, value, RpcMode.normal.name())));

    // allow reading
    Assertions.assertEquals(nbValues, asyncUtil.blockingGet(rpcClient.directoryValues(key)).length);
  }

  @Test
  public void write_on_readOnly_allow() throws Exception {
    String key = "com.samourai.whirlpool.ro";
    String value = "valueOne";

    // enable signature
    setAuthentication();

    // allow remove with signature
    asyncUtil.blockingGet(rpcClient.directoryRemove(key, value));
    Assertions.assertTrue(asyncUtil.blockingGet(rpcClient.directoryValues(key)).length == 0);

    // allow writing with signature
    asyncUtil.blockingGet(rpcClient.directoryAdd(key, value, RpcMode.normal.name()));

    // allow reading
    Assertions.assertTrue(asyncUtil.blockingGet(rpcClient.directoryValues(key)).length == 1);
  }

  @Test
  public void directoryValueWait_success() throws Exception {
    String key = "key";
    String value = "valueOne";

    // cleanup
    asyncUtil.blockingGet(rpcClient.directoryRemove(key, value));

    // add after 2 seconds
    executor.schedule(
        () -> {
          try {
            log.info("directoryAdd");
            asyncUtil.blockingGet(rpcClient.directoryAdd(key, value, RpcMode.normal.name()));
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
    String key = "key";
    String value = "valueOne";

    // cleanup
    asyncUtil.blockingGet(rpcClient.directoryRemove(key, value));

    // add too late
    executor.schedule(
        () -> {
          try {
            log.info("directoryAdd");
            asyncUtil.blockingGet(rpcClient.directoryAdd(key, value, RpcMode.normal.name()));
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
    String key = "key";
    String value = "valueOne";

    // cleanup
    asyncUtil.blockingGet(rpcClient.directoryRemove(key, value));

    // add after 2 seconds
    executor.schedule(
        () -> {
          try {
            log.info("directoryAdd");
            asyncUtil.blockingGet(rpcClient.directoryAdd(key, value, RpcMode.normal.name()));
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
  }
}
