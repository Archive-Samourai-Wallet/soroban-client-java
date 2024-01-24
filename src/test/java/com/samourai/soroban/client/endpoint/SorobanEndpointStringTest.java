package com.samourai.soroban.client.endpoint;

import com.samourai.soroban.client.AbstractTest;
import com.samourai.soroban.client.endpoint.string.SorobanEndpointString;
import com.samourai.soroban.client.rpc.RpcMode;
import com.samourai.soroban.client.wrapper.SorobanWrapperEncrypt;
import com.samourai.soroban.client.wrapper.SorobanWrapperSign;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SorobanEndpointStringTest extends AbstractTest {

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  @Test
  public void clear() throws Exception {
    String payload = "HELLO WORLD";
    SorobanEndpointString endpoint = new SorobanEndpointString(app, "CLEAR", RpcMode.SHORT);

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
            .getPayload();
    Assertions.assertEquals(payload, result);
  }

  @Test
  public void signed() throws Exception {
    String payload = "HELLO WORLD";
    SorobanEndpointString endpoint =
        new SorobanEndpointString(app, "SIGNED", RpcMode.SHORT, new SorobanWrapperSign());

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
            .getPayload();
    Assertions.assertEquals(payload, result);
  }

  @Test
  public void encrypted() throws Exception {
    String payload = "HELLO WORLD";
    SorobanEndpointString endpoint =
        new SorobanEndpointString(
            app, "ENCRYPTED", RpcMode.SHORT, new SorobanWrapperEncrypt(paymentCodeCounterparty));

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
            .getPayload();
    Assertions.assertEquals(payload, result);
  }
}
