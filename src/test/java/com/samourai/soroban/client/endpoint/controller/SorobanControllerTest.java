package com.samourai.soroban.client.endpoint.controller;

import com.samourai.soroban.client.AbstractTest;
import com.samourai.soroban.client.SorobanPayloadable;
import com.samourai.soroban.client.endpoint.meta.typed.SorobanEndpointTyped;
import com.samourai.soroban.client.endpoint.meta.typed.SorobanItemTyped;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMeta;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaNonce;
import com.samourai.soroban.client.rpc.RpcMode;
import com.samourai.soroban.client.rpc.TestPayload;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanControllerTest extends AbstractTest {
  private static final Logger log = LoggerFactory.getLogger(SorobanControllerTest.class);

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  private SorobanControllerTyped computeController(SorobanEndpointTyped endpoint) {
    return new SorobanControllerTyped(0, "testController", rpcSessionCounterparty, endpoint) {

      @Override
      protected SorobanPayloadable computeReplyOnRequestNew(SorobanItemTyped request, String key)
          throws Exception {
        return null;
      }

      @Override
      protected SorobanPayloadable computeReplyOnRequestExisting(
          SorobanItemTyped request, String key) throws Exception {
        return null;
      }
    };
  }

  @Test
  public void test_simple() throws Exception {
    SorobanEndpointTyped endpoint =
        new SorobanEndpointTyped(app, "CLEAR", RpcMode.SHORT, new SorobanWrapperMeta[] {});
    SorobanControllerTyped controller = computeController(endpoint);

    TestPayload initiator1 = new TestPayload("initiator1");
    TestPayload initiator1Dup = new TestPayload("initiator1");
    TestPayload initiator2 = new TestPayload("initiator2");
    TestPayload initiator2Dup = new TestPayload("initiator2");

    // send first payloads
    rpcSessionInitiator.withSorobanClient(
        sorobanClient -> {
          asyncUtil.blockingAwait(endpoint.send(sorobanClient, initiator1)); // new
          asyncUtil.blockingAwait(
              endpoint.send(sorobanClient, initiator1Dup)); // dup ignored by soroban
          asyncUtil.blockingAwait(endpoint.send(sorobanClient, initiator2)); // new
          asyncUtil.blockingAwait(
              endpoint.send(sorobanClient, initiator2Dup)); // dup ignored by soroban
          return null;
        });
    waitSorobanDelay();

    // run controller: process 2/2 messages
    controller.runOrchestrator();
    // duplicate payloads were filtered by Soroban
    Assertions.assertEquals(2, controller.getNbMessages());
    Assertions.assertEquals(2, controller.getNbProcesseds());
    Assertions.assertEquals(0, controller.getNbExistings());
    Assertions.assertEquals(2, controller.getProcessedById().size());
    Assertions.assertEquals(0, controller.getNbIgnored());

    // send more payloads
    TestPayload initiator1New = new TestPayload("initiator1");
    TestPayload initiator2New = new TestPayload("initiator2");
    TestPayload initiator3 = new TestPayload("initiator3");
    rpcSessionInitiator.withSorobanClient(
        sorobanClient -> {
          asyncUtil.blockingAwait(endpoint.send(sorobanClient, initiator1New)); // existing
          asyncUtil.blockingAwait(endpoint.send(sorobanClient, initiator2New)); // existing
          asyncUtil.blockingAwait(endpoint.send(sorobanClient, initiator3)); // new
          return null;
        });

    waitSorobanDelay();

    // run controller: process 1/3 messages
    controller.runOrchestrator();
    Assertions.assertEquals(3, controller.getNbMessages());
    Assertions.assertEquals(1, controller.getNbProcesseds());
    Assertions.assertEquals(2, controller.getNbExistings());
    Assertions.assertEquals(3, controller.getProcessedById().size());
    Assertions.assertEquals(0, controller.getNbIgnored());
  }

  @Test
  public void test_nonce() throws Exception {
    SorobanEndpointTyped endpoint =
        new SorobanEndpointTyped(
            app, "TEST", RpcMode.SHORT, new SorobanWrapperMeta[] {new SorobanWrapperMetaNonce()});
    SorobanControllerTyped controller = computeController(endpoint);

    TestPayload initiator1 = new TestPayload("initiator1");
    TestPayload initiator1Dup = new TestPayload("initiator1");
    TestPayload initiator2 = new TestPayload("initiator2");
    TestPayload initiator2Dup = new TestPayload("initiator2");

    // send first payloads
    rpcSessionInitiator.withSorobanClient(
        sorobanClient -> {
          asyncUtil.blockingAwait(endpoint.send(sorobanClient, initiator1)); // new
          asyncUtil.blockingAwait(
              endpoint.send(sorobanClient, initiator1Dup)); // dup ignored by key
          asyncUtil.blockingAwait(endpoint.send(sorobanClient, initiator2)); // new
          asyncUtil.blockingAwait(
              endpoint.send(sorobanClient, initiator2Dup)); // dup ignored by key
          return null;
        });

    waitSorobanDelay();

    // run controller: process 2/4 messages
    controller.runOrchestrator();
    // we received all payloads (thanks to the nonce)
    Assertions.assertEquals(2, controller.getNbMessages());
    Assertions.assertEquals(2, controller.getNbProcesseds());
    Assertions.assertEquals(0, controller.getNbExistings());
    Assertions.assertEquals(2, controller.getProcessedById().size());
    Assertions.assertEquals(0, controller.getNbIgnored());

    // send more payloads: process 1/3 messages
    TestPayload initiator1New = new TestPayload("initiator1");
    TestPayload initiator2New = new TestPayload("initiator2");
    TestPayload initiator3 = new TestPayload("initiator3");
    rpcSessionInitiator.withSorobanClient(
        sorobanClient -> {
          asyncUtil.blockingAwait(endpoint.send(sorobanClient, initiator1New)); // existing
          asyncUtil.blockingAwait(endpoint.send(sorobanClient, initiator2New)); // existing
          asyncUtil.blockingAwait(endpoint.send(sorobanClient, initiator3)); // new
          return null;
        });

    waitSorobanDelay();

    // run controller
    controller.runOrchestrator();
    Assertions.assertEquals(3, controller.getNbMessages());
    Assertions.assertEquals(1, controller.getNbProcesseds());
    Assertions.assertEquals(2, controller.getNbExistings());
    Assertions.assertEquals(3, controller.getProcessedById().size());
    Assertions.assertEquals(0, controller.getNbIgnored());
  }
}
