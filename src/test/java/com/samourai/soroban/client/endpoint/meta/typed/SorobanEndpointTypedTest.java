package com.samourai.soroban.client.endpoint.meta.typed;

import com.samourai.soroban.client.AbstractTest;
import com.samourai.soroban.client.SorobanPayloadable;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaEncryptWithSender;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaFilterSender;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaSender;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaSignWithSender;
import com.samourai.soroban.client.endpoint.wrapper.SorobanWrapper;
import com.samourai.soroban.client.exception.FilterDeclinedSorobanException;
import com.samourai.soroban.client.rpc.RpcMode;
import com.samourai.soroban.client.rpc.TestPayload;
import com.samourai.soroban.client.rpc.TestResponsePayload;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import java.util.concurrent.TimeoutException;
import java.util.function.BiPredicate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SorobanEndpointTypedTest extends AbstractTest {
  private SorobanEndpointTyped endpoint;

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();

    endpoint =
        new SorobanEndpointTyped(
            app,
            "CLEAR",
            RpcMode.SHORT,
            new SorobanWrapper[] {new SorobanWrapperMetaSender()},
            new Class[] {TestPayload.class});
  }

  @Test
  public void clear() throws Exception {
    TestPayload payload = new TestPayload("REQUEST");
    TestPayload responsePayload = new TestPayload("RESPONSE");
    BiPredicate<SorobanPayloadable, SorobanItemTyped> equals =
        (s, i) -> {
          try {
            return i.getPayload().equals(s.toPayload());
          } catch (Exception e) {
            return false;
          }
        };

    doTestEndpoint2Ways(endpoint, endpoint, payload, responsePayload, equals);
  }

  @Test
  public void signed() throws Exception {
    SorobanEndpointTyped endpoint =
        new SorobanEndpointTyped(
            app,
            "SIGNED",
            RpcMode.SHORT,
            new SorobanWrapper[] {new SorobanWrapperMetaSignWithSender()},
            new Class[] {TestPayload.class});

    TestPayload payload = new TestPayload("REQUEST");
    TestPayload responsePayload = new TestPayload("RESPONSE");
    BiPredicate<SorobanPayloadable, SorobanItemTyped> equals =
        (s, i) -> {
          try {
            return i.getPayload().equals(s.toPayload());
          } catch (Exception e) {
            return false;
          }
        };

    doTestEndpoint2Ways(endpoint, endpoint, payload, responsePayload, equals);
  }

  @Test
  public void encrypted() throws Exception {
    SorobanEndpointTyped endpointInitiator =
        new SorobanEndpointTyped(
            app,
            "ENCRYPTED",
            RpcMode.SHORT,
            new SorobanWrapper[] {new SorobanWrapperMetaEncryptWithSender(paymentCodeCounterparty)},
            new Class[] {TestPayload.class});
    SorobanEndpointTyped endpointCounterparty =
        new SorobanEndpointTyped(
            app,
            "ENCRYPTED",
            RpcMode.SHORT,
            new SorobanWrapper[] {new SorobanWrapperMetaEncryptWithSender(paymentCodeInitiator)},
            new Class[] {TestPayload.class});

    TestPayload payload = new TestPayload("REQUEST");
    TestPayload responsePayload = new TestPayload("RESPONSE");
    BiPredicate<SorobanPayloadable, SorobanItemTyped> equals =
        (s, i) -> {
          try {
            return s.toPayload().equals(i.getPayload());
          } catch (Exception e) {
            return false;
          }
        };

    doTestEndpoint2Ways(endpointInitiator, endpointCounterparty, payload, responsePayload, equals);
  }

  @Test
  public void encrypted_invalid() throws Exception {
    TestPayload payload = new TestPayload("REQUEST");

    // encrypt for another paymentCode
    PaymentCode paymentCodeTemp =
        rpcClientService.generateRpcWallet().getBip47Account().getPaymentCode();
    SorobanEndpointTyped endpointInitiator =
        new SorobanEndpointTyped(
            app,
            "ENCRYPTED",
            RpcMode.SHORT,
            new SorobanWrapper[] {new SorobanWrapperMetaEncryptWithSender(paymentCodeTemp)},
            new Class[] {TestPayload.class});
    SorobanEndpointTyped endpointCounterparty =
        new SorobanEndpointTyped(
            app,
            "ENCRYPTED",
            RpcMode.SHORT,
            new SorobanWrapper[] {new SorobanWrapperMetaEncryptWithSender(paymentCodeInitiator)},
            new Class[] {TestPayload.class});

    doTestEndpointSkippedPayload(endpointInitiator, endpointCounterparty, payload);
  }

  @Test
  public void list() throws Exception {
    TestPayload payload1 = new TestPayload("payload1");
    TestPayload payload2 = new TestPayload("payload2");
    TestPayload payload3 = new TestPayload("payload3");
    TestPayload payload4 = new TestPayload("payload4");
    SorobanEndpointTyped endpoint =
        new SorobanEndpointTyped(
            app, "CLEAR", RpcMode.SHORT, new SorobanWrapper[] {}, new Class[] {TestPayload.class});

    doTestEndpoint2WaysList(
        endpoint, endpoint, new TestPayload[] {payload1, payload2, payload3, payload4});
  }

  @Test
  public void remove_clear() throws Exception {
    TestPayload payload1 = new TestPayload("payload1");
    TestPayload payload2 = new TestPayload("payload2");
    SorobanEndpointTyped endpoint =
        new SorobanEndpointTyped(
            app, "CLEAR", RpcMode.SHORT, new SorobanWrapper[] {}, new Class[] {TestPayload.class});

    doTestEndpointDelete(endpoint, endpoint, payload1, payload2);
  }

  @Test
  public void remove_encrypted() throws Exception {
    TestPayload payload1 = new TestPayload("payload1");
    TestPayload payload2 = new TestPayload("payload2");
    SorobanEndpointTyped endpoint =
        new SorobanEndpointTyped(
            app,
            "ENCRYPTED",
            RpcMode.SHORT,
            new SorobanWrapper[] {new SorobanWrapperMetaEncryptWithSender(paymentCodeCounterparty)},
            new Class[] {TestPayload.class});

    doTestEndpointDelete(endpoint, endpoint, payload1, payload2);
  }

  @Test
  public void waitNext() throws Exception {
    TestPayload payload = new TestPayload("HELLO WORLD");

    // send request
    asyncUtil.blockingAwait(
        rpcSessionInitiator.withSorobanClient(
            sorobanClient -> endpoint.send(sorobanClient, payload)));
    waitSorobanDelay();

    // wait request
    SorobanItemTyped request =
        asyncUtil.blockingGet(endpoint.waitNext(rpcSessionCounterparty), 10000);
    Assertions.assertEquals(payload.getMessage(), request.read(TestPayload.class).getMessage());
    Assertions.assertEquals(payload.getMessage(), request.readOn(TestPayload.class).getMessage());

    // entry should be removed
    Assertions.assertThrows(
        TimeoutException.class,
        () -> {
          asyncUtil.blockingGet(endpoint.waitNext(rpcSessionCounterparty), 1000);
        });
  }

  @Test
  public void waitNext_typed() throws Exception {
    SorobanEndpointTyped endpointMulti =
        new SorobanEndpointTyped(
            app,
            "CLEAR",
            RpcMode.SHORT,
            new SorobanWrapper[] {},
            new Class[] {TestPayload.class, TestResponsePayload.class});
    TestPayload payload = new TestPayload("HELLO WORLD");

    // send unexpected request
    asyncUtil.blockingAwait(
        rpcSessionInitiator.withSorobanClient(
            sorobanClient ->
                endpointMulti.send(sorobanClient, new TestResponsePayload("UNEXPECTED"))));

    // send expected request
    asyncUtil.blockingAwait(
        rpcSessionInitiator.withSorobanClient(
            sorobanClient -> endpointMulti.send(sorobanClient, payload)));
    waitSorobanDelay();

    // wait request of specific type
    TestPayload request =
        asyncUtil.blockingGet(
            endpointMulti.waitNext(rpcSessionCounterparty, TestPayload.class), 10000);
    Assertions.assertEquals(payload.getMessage(), request.getMessage());
  }

  // REPLY

  @Test
  public void loopSendUntilReply_sameType() throws Exception {
    TestPayload payload = new TestPayload("HELLO WORLD");

    // wait request & send response
    new Thread(
            () -> {
              try {
                // wait request
                SorobanItemTyped request =
                    asyncUtil.blockingGet(endpoint.waitNext(rpcSessionCounterparty), 10000);

                // send response
                asyncUtil.blockingAwait(
                    rpcSessionInitiator.withSorobanClient(
                        sorobanClient ->
                            request
                                .getEndpointReply()
                                .send(
                                    sorobanClient,
                                    new TestPayload(
                                        request.read(TestPayload.class).getMessage()
                                            + " RESPONSE"))));
              } catch (Exception e) {
                Assertions.fail(e);
              }
            })
        .start();

    // send request & wait response
    TestPayload response =
        asyncUtil.blockingGet(
            endpoint.loopSendUntilReplyObject(
                rpcSessionInitiator, payload, 3000, TestPayload.class),
            5000);
    Assertions.assertEquals("HELLO WORLD RESPONSE", response.getMessage());
  }

  @Test
  public void loopSendUntilReply_mixedTypes() throws Exception {
    TestPayload payload = new TestPayload("HELLO WORLD");

    // wait request & send response
    new Thread(
            () -> {
              try {
                // wait request
                SorobanItemTyped request =
                    asyncUtil.blockingGet(endpoint.waitNext(rpcSessionCounterparty), 10000);

                // send response
                asyncUtil.blockingAwait(
                    rpcSessionInitiator.withSorobanClient(
                        sorobanClient ->
                            request
                                .getEndpointReply()
                                .send(
                                    sorobanClient,
                                    new TestResponsePayload(
                                        request.read(TestPayload.class).getMessage()
                                            + " RESPONSE"))));
              } catch (Exception e) {
                Assertions.fail(e);
              }
            })
        .start();

    // send request & wait response
    TestResponsePayload response =
        asyncUtil.blockingGet(
            endpoint.loopSendUntilReplyObject(
                rpcSessionInitiator, payload, 3000, TestResponsePayload.class),
            5000);
    Assertions.assertEquals("HELLO WORLD RESPONSE", response.getResponseMessage());
  }

  @Test
  public void loopSendUntilReply_timeout() throws Exception {
    TestPayload payload = new TestPayload("HELLO WORLD");
    Assertions.assertThrows(
        TimeoutException.class,
        () ->
            asyncUtil.blockingGet(
                endpoint.loopSendUntilReplyObject(
                    rpcSessionInitiator, payload, 1000, TestPayload.class),
                2000));
  }

  @Test
  public void sendAndWaitReply_timeout() throws Exception {
    TestPayload payload = new TestPayload("HELLO WORLD");
    Assertions.assertThrows(
        TimeoutException.class,
        () -> asyncUtil.blockingGet(endpoint.sendAndWaitReply(rpcSessionInitiator, payload, 1000)));
  }

  @Test
  public void sendAndWaitReply_success() throws Exception {
    // wait request & send response
    new Thread(
            () -> {
              try {
                // wait request
                SorobanItemTyped request =
                    asyncUtil.blockingGet(endpoint.waitNext(rpcSessionCounterparty), 10000);

                // send response
                asyncUtil.blockingAwait(
                    rpcSessionInitiator.withSorobanClient(
                        sorobanClient ->
                            request
                                .getEndpointReply()
                                .send(
                                    sorobanClient,
                                    new TestResponsePayload(
                                        request.read(TestPayload.class).getMessage()
                                            + " RESPONSE"))));
              } catch (Exception e) {
                Assertions.fail(e);
              }
            })
        .start();

    TestPayload payload = new TestPayload("HELLO WORLD");
    TestResponsePayload response =
        asyncUtil
            .blockingGet(endpoint.sendAndWaitReply(rpcSessionInitiator, payload, 5000))
            .read(TestResponsePayload.class);
    Assertions.assertEquals("HELLO WORLD RESPONSE", response.getResponseMessage());
  }

  @Test
  public void filterSender() throws Exception {
    endpoint =
        new SorobanEndpointTyped(
            app,
            "CLEAR",
            RpcMode.SHORT,
            new SorobanWrapper[] {new SorobanWrapperMetaFilterSender(paymentCodeInitiator)},
            new Class[] {TestPayload.class});
    SorobanEndpointTyped endpointBypass =
        new SorobanEndpointTyped(
            app,
            "CLEAR",
            RpcMode.SHORT,
            new SorobanWrapper[] {new SorobanWrapperMetaSender()},
            new Class[] {TestPayload.class});
    TestPayload payload = new TestPayload("HELLO WORLD");

    // send payload: initiator allowed
    asyncUtil.blockingAwait(
        rpcSessionInitiator.withSorobanClient(
            sorobanClient -> endpoint.send(sorobanClient, payload)));

    // send payload: counterparty declined
    Assertions.assertThrows(
        FilterDeclinedSorobanException.class,
        () -> {
          asyncUtil.blockingAwait(
              rpcSessionCounterparty.withSorobanClient(
                  sorobanClient -> endpoint.send(sorobanClient, payload)));
        });

    // send payload: forced from counterparty
    asyncUtil.blockingAwait(
        rpcSessionCounterparty.withSorobanClient(
            sorobanClient -> endpointBypass.send(sorobanClient, payload)));

    waitSorobanDelay();

    // get payload: invalid sender ignored
    SorobanListTyped result =
        asyncUtil.blockingGet(
            rpcSessionCounterparty.withSorobanClient(
                sorobanClient -> endpoint.getList(sorobanClient)));
    Assertions.assertEquals(1, result.size());
  }
}
