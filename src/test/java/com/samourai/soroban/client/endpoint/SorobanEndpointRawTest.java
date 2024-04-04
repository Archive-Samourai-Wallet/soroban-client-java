package com.samourai.soroban.client.endpoint;

import com.samourai.soroban.client.AbstractTest;
import com.samourai.soroban.client.endpoint.wrapper.SorobanWrapperString;
import com.samourai.soroban.client.rpc.RpcMode;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import java.util.function.BiPredicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SorobanEndpointRawTest extends AbstractTest {

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  @Test
  public void clear() throws Exception {
    String payload = "REQUEST";
    String responsePayload = "RESPONSE";
    BiPredicate<String, String> equals = (s, i) -> i.equals(s);

    SorobanEndpointRaw endpoint =
        new SorobanEndpointRaw(app.getDir("TEST"), RpcMode.SHORT, new SorobanWrapperString[] {});

    doTestEndpointReply(endpoint, endpoint, payload, responsePayload, equals);
  }

  @Test
  public void encrypted() throws Exception {
    String payload = "REQUEST";
    String responsePayload = "RESPONSE";
    BiPredicate<String, String> equals = (s, i) -> i.equals(s);

    SorobanEndpointRaw endpointInitiator =
        new SorobanEndpointRaw(app.getDir("TEST"), RpcMode.SHORT, new SorobanWrapperString[] {})
            .setEncryptTo(paymentCodeCounterparty);
    SorobanEndpointRaw endpointCounterparty =
        new SorobanEndpointRaw(app.getDir("TEST"), RpcMode.SHORT, new SorobanWrapperString[] {})
            .setDecryptFrom(paymentCodeInitiator);

    doTestEndpointReply(endpointInitiator, endpointCounterparty, payload, responsePayload, equals);
  }

  @Test
  public void encrypted_invalid() throws Exception {
    String payload = "REQUEST";

    // encrypt for another paymentCode
    PaymentCode paymentCodeTemp =
        sorobanConfig.getRpcClientService().generateRpcWallet().getBip47Account().getPaymentCode();
    SorobanEndpointRaw endpointInitiator =
        new SorobanEndpointRaw(app.getDir("TEST"), RpcMode.SHORT, new SorobanWrapperString[] {})
            .setEncryptTo(paymentCodeTemp);
    SorobanEndpointRaw endpointCounterparty =
        new SorobanEndpointRaw(app.getDir("TEST"), RpcMode.SHORT, new SorobanWrapperString[] {})
            .setDecryptFrom(paymentCodeInitiator);

    doTestEndpointSkippedPayload(endpointInitiator, endpointCounterparty, payload);
  }

  @Test
  public void list() throws Exception {
    String payload1 = "payloadInitiator1";
    String payload2 = "payloadInitiator2";
    String payload3 = "payloadCounterparty1";
    String payload4 = "payloadCounterparty2";
    SorobanEndpointRaw endpoint =
        new SorobanEndpointRaw(app.getDir("TEST"), RpcMode.SHORT, new SorobanWrapperString[] {});

    doTestEndpoint2WaysList(
        endpoint, endpoint, new String[] {payload1, payload2, payload3, payload4});
  }

  @Test
  public void remove_clear() throws Exception {
    String payload1 = "payloadInitiator1";
    String payload2 = "payloadInitiator2";
    SorobanEndpointRaw endpoint =
        new SorobanEndpointRaw(app.getDir("TEST"), RpcMode.SHORT, new SorobanWrapperString[] {});

    doTestEndpointDelete(endpoint, endpoint, payload1, payload2);
  }
}
