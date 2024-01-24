package com.samourai.soroban.client.endpoint;

import com.samourai.soroban.client.AbstractTest;
import com.samourai.soroban.client.endpoint.meta.typed.SorobanEndpointTyped;
import com.samourai.soroban.client.endpoint.meta.typed.SorobanItemTyped;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMeta;
import com.samourai.soroban.client.rpc.RpcMode;
import com.samourai.soroban.client.rpc.TestPayload;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractSorobanEndpointTest extends AbstractTest {
  private static final Logger log = LoggerFactory.getLogger(AbstractSorobanEndpointTest.class);
  private SorobanEndpointTyped endpoint;

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();
    endpoint = new SorobanEndpointTyped(app, "CLEAR", RpcMode.SHORT, new SorobanWrapperMeta[] {});
  }

  @Test
  public void waitNext() throws Exception {
    TestPayload payload = new TestPayload("HELLO WORLD");

    // send payload (delayed)
    runDelayed(
        1000,
        () -> {
          try {
            asyncUtil.blockingAwait(
                rpcSessionInitiator.withSorobanClient(
                    sorobanClient -> endpoint.send(sorobanClient, payload)));
          } catch (Exception e) {
            Assertions.fail(e);
          }
        });

    // wait payload
    TestPayload result =
        asyncUtil
            .blockingGet(endpoint.waitNext(rpcSessionCounterparty), 2000)
            .read(TestPayload.class);
    Assertions.assertEquals(payload.getMessage(), result.getMessage());
  }

  @Test
  public void delete() throws Exception {
    TestPayload payload1 = new TestPayload("payload1");
    TestPayload payload2 = new TestPayload("payload2");

    asyncUtil.blockingAwait(
        rpcSessionInitiator.withSorobanClient(
            sorobanClient -> endpoint.send(sorobanClient, payload1)));
    asyncUtil.blockingAwait(
        rpcSessionInitiator.withSorobanClient(
            sorobanClient -> endpoint.send(sorobanClient, payload2)));

    waitSorobanDelay();

    // list
    List<SorobanItemTyped> result =
        asyncUtil
            .blockingGet(
                rpcSessionCounterparty.withSorobanClient(
                    sorobanClient -> endpoint.getList(sorobanClient)))
            .getList();
    Assertions.assertEquals(2, result.size());

    // delete 1
    asyncUtil.blockingAwait(
        rpcSessionCounterparty.withSorobanClient(
            sorobanClient -> endpoint.delete(sorobanClientCounterparty, result.get(0))));
    waitSorobanDelay();

    // list
    List<SorobanItemTyped> resultNew =
        asyncUtil
            .blockingGet(
                rpcSessionCounterparty.withSorobanClient(
                    sorobanClient -> endpoint.getList(sorobanClient)))
            .getList();
    Assertions.assertEquals(1, resultNew.size());
  }
}
