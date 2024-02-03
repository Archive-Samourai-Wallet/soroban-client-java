package com.samourai.soroban.client.endpoint.meta;

import com.samourai.soroban.client.AbstractTest;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaEncryptWithSender;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaSender;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaSignWithSender;
import com.samourai.soroban.client.endpoint.wrapper.SorobanWrapper;
import com.samourai.soroban.client.endpoint.wrapper.SorobanWrapperString;
import com.samourai.soroban.client.rpc.RpcMode;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import java.util.function.BiPredicate;
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
    String payload = "REQUEST";
    String responsePayload = "RESPONSE";
    BiPredicate<String, SorobanItem> equals = (s, i) -> i.getPayload().equals(s);

    SorobanEndpointMetaString endpoint =
        new SorobanEndpointMetaString(
            app, "CLEAR", RpcMode.SHORT, new SorobanWrapper[] {new SorobanWrapperMetaSender()});

    doTestEndpoint2Ways(endpoint, endpoint, payload, responsePayload, equals);
  }

  @Test
  public void signed() throws Exception {
    String payload = "REQUEST";
    String responsePayload = "RESPONSE";
    BiPredicate<String, SorobanItem> equals = (s, i) -> i.getPayload().equals(s);

    SorobanEndpointMetaString endpoint =
        new SorobanEndpointMetaString(
            app,
            "SIGNED",
            RpcMode.SHORT,
            new SorobanWrapper[] {new SorobanWrapperMetaSignWithSender()});

    doTestEndpoint2Ways(endpoint, endpoint, payload, responsePayload, equals);
  }

  @Test
  public void encrypted() throws Exception {
    String payload = "REQUEST";
    String responsePayload = "RESPONSE";
    BiPredicate<String, SorobanItem> equals = (s, i) -> i.getPayload().equals(s);

    SorobanEndpointMetaString endpointInitiator =
        new SorobanEndpointMetaString(
            app,
            "ENCRYPTED",
            RpcMode.SHORT,
            new SorobanWrapper[] {
              new SorobanWrapperMetaEncryptWithSender(paymentCodeCounterparty)
            });
    SorobanEndpointMetaString endpointCounterparty =
        new SorobanEndpointMetaString(
            app,
            "ENCRYPTED",
            RpcMode.SHORT,
            new SorobanWrapper[] {new SorobanWrapperMetaEncryptWithSender(paymentCodeInitiator)});

    doTestEndpoint2Ways(endpointInitiator, endpointCounterparty, payload, responsePayload, equals);
  }

  @Test
  public void encrypted_invalid() throws Exception {
    String payload = "REQUEST";

    // encrypt for another paymentCode
    PaymentCode paymentCodeTemp =
        rpcClientService.generateRpcWallet().getBip47Account().getPaymentCode();
    SorobanEndpointMetaString endpointInitiator =
        new SorobanEndpointMetaString(
            app,
            "ENCRYPTED",
            RpcMode.SHORT,
            new SorobanWrapper[] {new SorobanWrapperMetaEncryptWithSender(paymentCodeTemp)});
    SorobanEndpointMetaString endpointCounterparty =
        new SorobanEndpointMetaString(
            app,
            "ENCRYPTED",
            RpcMode.SHORT,
            new SorobanWrapper[] {new SorobanWrapperMetaEncryptWithSender(paymentCodeInitiator)});

    doTestEndpointSkippedPayload(endpointInitiator, endpointCounterparty, payload);
  }

  @Test
  public void list() throws Exception {
    String payload1 = "payloadInitiator1";
    String payload2 = "payloadInitiator2";
    String payload3 = "payloadCounterparty1";
    String payload4 = "payloadCounterparty2";
    SorobanEndpointMetaString endpoint =
        new SorobanEndpointMetaString(app, "CLEAR", RpcMode.SHORT, new SorobanWrapper[] {});

    doTestEndpoint2WaysList(
        endpoint, endpoint, new String[] {payload1, payload2, payload3, payload4});
  }

  @Test
  public void remove_clear() throws Exception {
    String payload1 = "payloadInitiator1";
    String payload2 = "payloadInitiator2";
    SorobanEndpointMetaString endpoint =
        new SorobanEndpointMetaString(app, "CLEAR", RpcMode.SHORT, new SorobanWrapper[] {});

    doTestEndpointDelete(endpoint, endpoint, payload1, payload2);
  }

  @Test
  public void remove_encrypted() throws Exception {
    String payload1 = "payloadInitiator1";
    String payload2 = "payloadInitiator2";

    SorobanEndpointMetaString endpointInitiator =
        new SorobanEndpointMetaString(
            app,
            "ENCRYPTED",
            RpcMode.SHORT,
            new SorobanWrapper[] {
              new SorobanWrapperMetaEncryptWithSender(paymentCodeCounterparty)
            });
    SorobanEndpointMetaString endpointCounterparty =
        new SorobanEndpointMetaString(
            app,
            "ENCRYPTED",
            RpcMode.SHORT,
            new SorobanWrapper[] {new SorobanWrapperMetaEncryptWithSender(paymentCodeInitiator)});

    doTestEndpointDelete(endpointInitiator, endpointCounterparty, payload1, payload2);
  }
}
