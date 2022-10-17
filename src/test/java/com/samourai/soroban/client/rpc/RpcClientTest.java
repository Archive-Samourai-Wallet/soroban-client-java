package com.samourai.soroban.client.rpc;

import com.samourai.soroban.client.AbstractTest;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcClientTest extends AbstractTest {
  private static final Logger log = LoggerFactory.getLogger(RpcClientTest.class);
  private static final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(5);

  private RpcClient rpcClient;

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();
    this.rpcClient = rpcService.createRpcClient(params);
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
