package com.samourai.soroban.client.endpoint;

import com.samourai.soroban.client.AbstractTest;
import com.samourai.soroban.client.endpoint.typed.SorobanEndpointTyped;
import com.samourai.soroban.client.endpoint.typed.SorobanPayloadTyped;
import com.samourai.soroban.client.rpc.RpcMode;
import com.samourai.soroban.client.rpc.TestPayload;
import com.samourai.soroban.client.wrapper.SorobanWrapperEncrypt;
import com.samourai.soroban.client.wrapper.SorobanWrapperSign;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Predicate;

public class SorobanEndpointTypedTest extends AbstractTest {

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  @Test
  public void clear() throws Exception {
    TestPayload payload = new TestPayload("HELLO WORLD");
    SorobanEndpointTyped<TestPayload> endpoint = new SorobanEndpointTyped(app, "CLEAR", RpcMode.SHORT);

    // send payload
    asyncUtil.blockingAwait(
        rpcSessionInitiator.withSorobanClient(
            sorobanClient -> endpoint.send(sorobanClient, payload)));

    // get payload
    TestPayload result =
            asyncUtil
                .blockingGet(
                    rpcSessionCounterparty.withSorobanClient(
                        sorobanClient -> endpoint.getNext(sorobanClient)))
                .getPayloadTyped();
    Assertions.assertEquals(payload.getMessage(), result.getMessage());
  }

  @Test
  public void signed() throws Exception {
    TestPayload payload = new TestPayload("HELLO WORLD");
    SorobanEndpointTyped<TestPayload> endpoint =
        new SorobanEndpointTyped(app, "SIGNED", RpcMode.SHORT, new SorobanWrapperSign());

    // send payload
    asyncUtil.blockingAwait(
        rpcSessionInitiator.withSorobanClient(
            sorobanClient -> endpoint.send(sorobanClient, payload)));

    // get payload
    TestPayload result =
            asyncUtil
                .blockingGet(
                    rpcSessionCounterparty.withSorobanClient(
                        sorobanClient -> endpoint.getNext(sorobanClient)))
                .getPayloadTyped();
    Assertions.assertEquals(payload.getMessage(), result.getMessage());
  }

  @Test
  public void encrypted() throws Exception {
    TestPayload payload = new TestPayload("HELLO WORLD");
    SorobanEndpointTyped<TestPayload> endpoint =
        new SorobanEndpointTyped(
            app, "ENCRYPTED", RpcMode.SHORT, new SorobanWrapperEncrypt(paymentCodeCounterparty));

    // send payload
    asyncUtil.blockingAwait(
        rpcSessionInitiator.withSorobanClient(
            sorobanClient -> endpoint.send(sorobanClient, payload)));

    // get payload
    TestPayload result =
            asyncUtil
                .blockingGet(
                    rpcSessionCounterparty.withSorobanClient(
                        sorobanClient -> endpoint.getNext(sorobanClient)))
                .getPayloadTyped();
    Assertions.assertEquals(payload.getMessage(), result.getMessage());
  }

  @Test
  public void list() throws Exception {
    TestPayload payloadInitiator1 = new TestPayload("payloadInitiator1");
    TestPayload payloadInitiator2 = new TestPayload("payloadInitiator2");
    TestPayload payloadCounterparty1 = new TestPayload("payloadCounterparty1");
    TestPayload payloadCounterparty2 = new TestPayload("payloadCounterparty2");
    SorobanEndpointTyped<TestPayload> endpoint = new SorobanEndpointTyped(app, "CLEAR", RpcMode.SHORT);

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
    List allResults = asyncUtil.blockingGet(
            rpcSessionCounterparty.withSorobanClient(sorobanClient ->
                            endpoint.getList(sorobanClient)));
    Assertions.assertEquals(4, allResults.size());

    // get payloads
    Predicate<SorobanPayloadTyped<TestPayload>> filterCounterparty = p ->
            p.getSender().equals(paymentCodeCounterparty.toString());
    List resultsCounterparty = asyncUtil.blockingGet(
                            rpcSessionCounterparty.withSorobanClient(sorobanClient ->
                                            endpoint.getList(sorobanClient, filterCounterparty)));
    Assertions.assertEquals(2, resultsCounterparty.size());

    // TODO listUntyped.distinct
  }
}
