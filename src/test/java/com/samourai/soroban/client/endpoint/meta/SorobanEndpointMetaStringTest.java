package com.samourai.soroban.client.endpoint.meta;

import com.samourai.soroban.client.AbstractTest;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaEncryptWithSender;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaSignWithSender;
import com.samourai.soroban.client.endpoint.wrapper.SorobanWrapper;
import com.samourai.soroban.client.endpoint.wrapper.SorobanWrapperString;
import com.samourai.soroban.client.rpc.RpcMode;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SorobanEndpointMetaStringTest extends AbstractTest {

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  @Test
  public void clear() throws Exception {
    String payload = "HELLO WORLD";
    SorobanEndpointMetaString endpoint =
        new SorobanEndpointMetaString(app, "CLEAR", RpcMode.SHORT, new SorobanWrapperString[] {});

    // send payload
    asyncUtil.blockingAwait(
        rpcSessionInitiator.withSorobanClient(
            sorobanClient -> endpoint.send(sorobanClient, payload)));

    // get payload
    SorobanItem result =
        asyncUtil
            .blockingGet(
                rpcSessionCounterparty.withSorobanClient(
                    sorobanClient -> endpoint.getNext(sorobanClient)))
            .get();
    Assertions.assertEquals(payload, result.getEntry());
  }

  @Test
  public void signed() throws Exception {
    String payload = "HELLO WORLD";
    SorobanEndpointMetaString endpoint =
        new SorobanEndpointMetaString(
            app,
            "SIGNED",
            RpcMode.SHORT,
            new SorobanWrapper[] {new SorobanWrapperMetaSignWithSender()});

    // send payload
    asyncUtil.blockingAwait(
        rpcSessionInitiator.withSorobanClient(
            sorobanClient -> endpoint.send(sorobanClient, payload)));

    // get payload
    SorobanItem result =
        asyncUtil
            .blockingGet(
                rpcSessionCounterparty.withSorobanClient(
                    sorobanClient -> endpoint.getNext(sorobanClient)))
            .get();
    Assertions.assertEquals(payload, result.getEntry());
  }

  @Test
  public void encrypted() throws Exception {
    String payload = "HELLO WORLD";
    SorobanEndpointMetaString endpoint =
        new SorobanEndpointMetaString(
            app,
            "ENCRYPTED",
            RpcMode.SHORT,
            new SorobanWrapper[] {
              new SorobanWrapperMetaEncryptWithSender(paymentCodeCounterparty)
            });

    // send payload
    asyncUtil.blockingAwait(
        rpcSessionInitiator.withSorobanClient(
            sorobanClient -> endpoint.send(sorobanClient, payload)));

    // get payload
    SorobanItem result =
        asyncUtil
            .blockingGet(
                rpcSessionCounterparty.withSorobanClient(
                    sorobanClient -> endpoint.getNext(sorobanClient)))
            .get();
    Assertions.assertEquals(payload, result.getEntry());
  }

  @Test
  public void encrypted_invalid() throws Exception {
    String payload = "HELLO WORLD";

    // encrypt for another paymentCode
    PaymentCode paymentCodeTemp =
        rpcClientService.generateRpcWallet().getBip47Account().getPaymentCode();
    SorobanEndpointMetaString endpoint =
        new SorobanEndpointMetaString(
            app,
            "ENCRYPTED",
            RpcMode.SHORT,
            new SorobanWrapper[] {new SorobanWrapperMetaEncryptWithSender(paymentCodeTemp)});

    // send payload
    asyncUtil.blockingAwait(
        rpcSessionInitiator.withSorobanClient(
            sorobanClient -> endpoint.send(sorobanClient, payload)));

    // get payload
    Assertions.assertFalse(
        asyncUtil
            .blockingGet(
                rpcSessionCounterparty.withSorobanClient(
                    sorobanClient -> endpoint.getNext(sorobanClient)))
            .isPresent());
  }

  @Test
  public void list() throws Exception {
    String payloadInitiator1 = "payloadInitiator1";
    String payloadInitiator2 = "payloadInitiator2";
    String payloadCounterparty1 = "payloadCounterparty1";
    String payloadCounterparty2 = "payloadCounterparty2";
    SorobanEndpointMetaString endpoint =
        new SorobanEndpointMetaString(app, "CLEAR", RpcMode.SHORT, new SorobanWrapperString[] {});

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
    SorobanList<SorobanItem> allResults =
        asyncUtil.blockingGet(
            rpcSessionCounterparty.withSorobanClient(
                sorobanClient -> endpoint.getList(sorobanClient)));
    Assertions.assertEquals(4, allResults.size());
  }
}
