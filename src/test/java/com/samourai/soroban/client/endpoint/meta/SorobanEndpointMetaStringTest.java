package com.samourai.soroban.client.endpoint.meta;

import com.samourai.soroban.client.AbstractTest;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaAuthWithSamouraiSender;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaSender;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaSign;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaSignWithSender;
import com.samourai.soroban.client.endpoint.wrapper.SorobanWrapper;
import com.samourai.soroban.client.rpc.RpcMode;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import java.util.function.BiPredicate;
import org.bitcoinj.core.ECKey;
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
            app.getDir("TEST"),
            RpcMode.SHORT,
            new SorobanWrapper[] {new SorobanWrapperMetaSender()});

    doTestEndpointReply(endpoint, endpoint, payload, responsePayload, equals);
  }

  @Test
  public void signWithSender() throws Exception {
    String payload = "REQUEST";
    String responsePayload = "RESPONSE";
    BiPredicate<String, SorobanItem> equals = (s, i) -> i.getPayload().equals(s);

    SorobanEndpointMetaString endpoint =
        new SorobanEndpointMetaString(
            app.getDir("TEST"),
            RpcMode.SHORT,
            new SorobanWrapper[] {new SorobanWrapperMetaSignWithSender()});

    doTestEndpointReply(endpoint, endpoint, payload, responsePayload, equals);
  }

  @Test
  public void sign() throws Exception {
    String payload = "REQUEST";
    BiPredicate<String, SorobanItem> equals = (s, i) -> i.getPayload().equals(s);

    ECKey signingKey = new ECKey();
    String signingAddress = signingKey.toAddress(params).toString();

    SorobanEndpointMetaString endpointInitiator =
        new SorobanEndpointMetaString(
            app.getDir("TEST"),
            RpcMode.SHORT,
            new SorobanWrapper[] {new SorobanWrapperMetaSign(signingKey, params)});

    SorobanEndpointMetaString endpointCounterparty =
        new SorobanEndpointMetaString(
            app.getDir("TEST"),
            RpcMode.SHORT,
            new SorobanWrapper[] {new SorobanWrapperMetaSign(signingAddress)});

    doTestEndpoint(endpointInitiator, endpointCounterparty, payload, equals);
  }

  @Test
  public void authWithSamouraiSender() throws Exception {
    String payload = "REQUEST";
    String responsePayload = "RESPONSE";
    BiPredicate<String, SorobanItem> equals = (s, i) -> i.getPayload().equals(s);

    String auth = messageSignUtil.signMessage(samouraiSigningKey, paymentCodeInitiator.toString());
    SorobanEndpointMetaString endpointCoordinator =
        new SorobanEndpointMetaString(
            app.getDir("TEST"),
            RpcMode.SHORT,
            new SorobanWrapper[] {
              new SorobanWrapperMetaSender(),
              new SorobanWrapperMetaAuthWithSamouraiSender(whirlpoolNetwork, auth)
            });

    SorobanEndpointMetaString endpointClient =
        new SorobanEndpointMetaString(
            app.getDir("TEST"),
            RpcMode.SHORT,
            new SorobanWrapper[] {
              new SorobanWrapperMetaSender(),
              new SorobanWrapperMetaAuthWithSamouraiSender(whirlpoolNetwork)
            });

    doTestEndpointReply(endpointCoordinator, endpointClient, payload, responsePayload, equals);
  }

  @Test
  public void authWithSamouraiSender_invalid() throws Exception {
    String payload = "REQUEST";

    // invalid auth
    String auth = messageSignUtil.signMessage(samouraiSigningKey, paymentCodeInitiator + "INVALID");
    SorobanEndpointMetaString endpointCoordinator =
        new SorobanEndpointMetaString(
            app.getDir("TEST"),
            RpcMode.SHORT,
            new SorobanWrapper[] {
              new SorobanWrapperMetaAuthWithSamouraiSender(whirlpoolNetwork, auth)
            });

    SorobanEndpointMetaString endpointClient =
        new SorobanEndpointMetaString(
            app.getDir("TEST"),
            RpcMode.SHORT,
            new SorobanWrapper[] {new SorobanWrapperMetaAuthWithSamouraiSender(whirlpoolNetwork)});

    doTestEndpointSkippedPayload(endpointCoordinator, endpointClient, payload);
  }

  @Test
  public void encrypted() throws Exception {
    String payload = "REQUEST";
    String responsePayload = "RESPONSE";
    BiPredicate<String, SorobanItem> equals = (s, i) -> i.getPayload().equals(s);

    SorobanEndpointMetaString endpointInitiator =
        new SorobanEndpointMetaString(app.getDir("TEST"), RpcMode.SHORT, new SorobanWrapper[] {})
            .setEncryptTo(paymentCodeCounterparty);

    SorobanEndpointMetaString endpointCounterparty =
        new SorobanEndpointMetaString(app.getDir("TEST"), RpcMode.SHORT, new SorobanWrapper[] {})
            .setDecryptFrom(paymentCodeInitiator);

    doTestEndpointReply(endpointInitiator, endpointCounterparty, payload, responsePayload, equals);
  }

  @Test
  public void encrypted_invalid() throws Exception {
    String payload = "REQUEST";

    // encrypt for another paymentCode
    PaymentCode paymentCodeTemp =
        rpcClientService.generateRpcWallet().getBip47Account().getPaymentCode();
    SorobanEndpointMetaString endpointInitiator =
        new SorobanEndpointMetaString(app.getDir("TEST"), RpcMode.SHORT, new SorobanWrapper[] {})
            .setEncryptTo(paymentCodeTemp);

    SorobanEndpointMetaString endpointCounterparty =
        new SorobanEndpointMetaString(app.getDir("TEST"), RpcMode.SHORT, new SorobanWrapper[] {})
            .setDecryptFrom(paymentCodeInitiator);

    doTestEndpointSkippedPayload(endpointInitiator, endpointCounterparty, payload);
  }

  @Test
  public void encryptWithSender() throws Exception {
    String payload = "REQUEST";
    String responsePayload = "RESPONSE";
    BiPredicate<String, SorobanItem> equals = (s, i) -> i.getPayload().equals(s);

    // encrypt to partner with sender
    SorobanEndpointMetaString endpointInitiator =
        new SorobanEndpointMetaString(app.getDir("TEST"), RpcMode.SHORT, new SorobanWrapper[] {})
            .setEncryptToWithSender(paymentCodeCounterparty);

    // counterparty decrypts from sender
    SorobanEndpointMetaString endpointCounterparty =
        new SorobanEndpointMetaString(app.getDir("TEST"), RpcMode.SHORT, new SorobanWrapper[] {})
            .setDecryptFromSender();

    doTestEndpointReply(endpointInitiator, endpointCounterparty, payload, responsePayload, equals);
  }

  @Test
  public void list() throws Exception {
    String payload1 = "payloadInitiator1";
    String payload2 = "payloadInitiator2";
    String payload3 = "payloadCounterparty1";
    String payload4 = "payloadCounterparty2";
    SorobanEndpointMetaString endpoint =
        new SorobanEndpointMetaString(app.getDir("TEST"), RpcMode.SHORT, new SorobanWrapper[] {});

    doTestEndpoint2WaysList(
        endpoint, endpoint, new String[] {payload1, payload2, payload3, payload4});
  }

  @Test
  public void remove_clear() throws Exception {
    String payload1 = "payloadInitiator1";
    String payload2 = "payloadInitiator2";
    SorobanEndpointMetaString endpoint =
        new SorobanEndpointMetaString(app.getDir("TEST"), RpcMode.SHORT, new SorobanWrapper[] {});

    doTestEndpointDelete(endpoint, endpoint, payload1, payload2);
  }

  @Test
  public void remove_encrypted() throws Exception {
    String payload1 = "payloadInitiator1";
    String payload2 = "payloadInitiator2";

    SorobanEndpointMetaString endpointInitiator =
        new SorobanEndpointMetaString(app.getDir("TEST"), RpcMode.SHORT, new SorobanWrapper[] {})
            .setEncryptTo(paymentCodeCounterparty);

    SorobanEndpointMetaString endpointCounterparty =
        new SorobanEndpointMetaString(app.getDir("TEST"), RpcMode.SHORT, new SorobanWrapper[] {})
            .setDecryptFrom(paymentCodeInitiator);

    doTestEndpointDelete(endpointInitiator, endpointCounterparty, payload1, payload2);
  }
}
