package com.samourai.soroban.client.endpoint;

import com.samourai.soroban.client.AbstractTest;
import com.samourai.soroban.client.endpoint.wrapper.SorobanWrapperString;
import com.samourai.soroban.client.rpc.RpcMode;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SorobanEndpointRawTest extends AbstractTest {

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  @Test
  public void test() throws Exception {
    String payload = "HELLO WORLD";
    SorobanEndpointRaw endpoint =
        new SorobanEndpointRaw(app, "RAW", RpcMode.SHORT, new SorobanWrapperString[] {});

    // send payload
    asyncUtil.blockingAwait(
        rpcSessionInitiator.withSorobanClient(
            sorobanClient -> endpoint.send(sorobanClient, payload)));

    // get payload
    String result =
        asyncUtil
            .blockingGet(
                rpcSessionCounterparty.withSorobanClient(
                    sorobanClient -> endpoint.getNext(sorobanClient)))
            .get();
    Assertions.assertEquals(payload, result);
  }

  @Test
  public void list() throws Exception {
    String payloadInitiator1 = "payloadInitiator1";
    String payloadInitiator2 = "payloadInitiator2";
    String payloadCounterparty1 = "payloadCounterparty1";
    String payloadCounterparty2 = "payloadCounterparty2";
    SorobanEndpointRaw endpoint =
        new SorobanEndpointRaw(app, "CLEAR", RpcMode.SHORT, new SorobanWrapperString[] {});

    // send payloads
    rpcSessionInitiator.withSorobanClient(
        sorobanClient -> {
          asyncUtil.blockingAwait(endpoint.send(sorobanClient, payloadInitiator1));
          asyncUtil.blockingAwait(endpoint.send(sorobanClient, payloadInitiator2));
          return null;
        });
    rpcSessionCounterparty.withSorobanClient(
        sorobanClient -> {
          asyncUtil.blockingAwait(endpoint.send(sorobanClient, payloadCounterparty1));
          asyncUtil.blockingAwait(endpoint.send(sorobanClient, payloadCounterparty2));
          return null;
        });

    // get all payloads
    List<String> allResults =
        asyncUtil.blockingGet(
            rpcSessionCounterparty.withSorobanClient(
                sorobanClient -> endpoint.getList(sorobanClient)));
    Assertions.assertEquals(4, allResults.size());
  }
}
