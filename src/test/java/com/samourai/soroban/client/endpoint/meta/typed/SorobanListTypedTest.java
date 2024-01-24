package com.samourai.soroban.client.endpoint.meta.typed;

import com.samourai.soroban.client.AbstractTest;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaNonce;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaSender;
import com.samourai.soroban.client.endpoint.wrapper.SorobanWrapper;
import com.samourai.soroban.client.rpc.RpcMode;
import com.samourai.soroban.client.rpc.TestPayload;
import com.samourai.soroban.client.rpc.TestResponsePayload;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanListTypedTest extends AbstractTest {
  private static final Logger log = LoggerFactory.getLogger(SorobanListTypedTest.class);

  private SorobanListTyped list;

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();

    TestPayload payloadInitiator1 = new TestPayload("initiator_1");
    TestPayload payloadInitiator2 = new TestPayload("initiator_2");
    TestResponsePayload payloadInitiator3 = new TestResponsePayload("initiator_3_response");
    TestResponsePayload payloadInitiator4 = new TestResponsePayload("initiator_4_response");
    TestPayload payloadCounterparty1 = new TestPayload("counterparty_1");
    TestPayload payloadCounterparty2 = new TestPayload("counterparty_2");
    TestResponsePayload payloadCounterparty3 = new TestResponsePayload("counterparty_3_response");
    TestResponsePayload payloadCounterparty4 = new TestResponsePayload("counterparty_4_response");

    SorobanEndpointTyped endpoint =
        new SorobanEndpointTyped(
            app,
            "CLEAR",
            RpcMode.SHORT,
            new SorobanWrapper[] {
              new SorobanWrapperMetaSender(), new SorobanWrapperMetaNonce(),
            });

    // send payloads
    rpcSessionInitiator.withSorobanClient(
        sorobanClient -> {
          asyncUtil.blockingAwait(endpoint.send(sorobanClient, payloadInitiator1));
          asyncUtil.blockingAwait(endpoint.send(sorobanClient, payloadInitiator2));
          asyncUtil.blockingAwait(endpoint.send(sorobanClient, payloadInitiator3));
          asyncUtil.blockingAwait(endpoint.send(sorobanClient, payloadInitiator4));
          return null;
        });
    rpcSessionCounterparty.withSorobanClient(
        sorobanClient -> {
          asyncUtil.blockingAwait(endpoint.send(sorobanClient, payloadCounterparty1));
          asyncUtil.blockingAwait(endpoint.send(sorobanClient, payloadCounterparty2));
          asyncUtil.blockingAwait(endpoint.send(sorobanClient, payloadCounterparty3));
          asyncUtil.blockingAwait(endpoint.send(sorobanClient, payloadCounterparty4));
          return null;
        });

    // get all payloads
    list =
        asyncUtil.blockingGet(
            rpcSessionCounterparty.withSorobanClient(
                sorobanClient -> endpoint.getList(sorobanClient)));
    Assertions.assertEquals(8, list.size());
  }

  @Test
  public void getList_filtered() throws Exception {
    // filter by sender
    Predicate<SorobanItemTyped> filterCounterparty =
        p -> p.getMetaSender().equals(paymentCodeCounterparty);

    List<SorobanItemTyped> results = list.getList(filterCounterparty);
    Assertions.assertEquals(4, results.size());
    Assertions.assertEquals("counterparty_1", results.get(0).read(TestPayload.class).getMessage());
    Assertions.assertEquals("counterparty_2", results.get(1).read(TestPayload.class).getMessage());
    Assertions.assertEquals(
        "counterparty_3_response",
        results.get(2).read(TestResponsePayload.class).getResponseMessage());
    Assertions.assertEquals(
        "counterparty_4_response",
        results.get(3).read(TestResponsePayload.class).getResponseMessage());
  }

  @Test
  public void getListObjects() throws Exception {
    List<TestPayload> results = list.getListObjects(TestPayload.class);
    Assertions.assertEquals(4, results.size());
  }

  @Test
  public void filterLatestBySender() throws Exception {
    List<SorobanItemTyped> results = list.distinctLatestBySender().getList();
    Assertions.assertEquals(2, results.size());

    Assertions.assertEquals(
        "initiator_4_response",
        results.get(0).read(TestResponsePayload.class).getResponseMessage());
    Assertions.assertEquals(
        "counterparty_4_response",
        results.get(1).read(TestResponsePayload.class).getResponseMessage());
  }

  @Test
  public void filterLatestBySenderAndType() throws Exception {
    List<SorobanItemTyped> results = list.filterLatestBySenderAndType(TestPayload.class).getList();
    Assertions.assertEquals(2, results.size());

    Assertions.assertEquals("initiator_2", results.get(0).read(TestPayload.class).getMessage());
    Assertions.assertEquals("counterparty_2", results.get(1).read(TestPayload.class).getMessage());
  }

  @Test
  public void filterByType() throws Exception {
    List<SorobanItemTyped> results = list.filterByType(TestPayload.class).getList();
    Assertions.assertEquals(4, results.size());

    Assertions.assertEquals("initiator_1", results.get(0).read(TestPayload.class).getMessage());
    Assertions.assertEquals("initiator_2", results.get(1).read(TestPayload.class).getMessage());
    Assertions.assertEquals("counterparty_1", results.get(2).read(TestPayload.class).getMessage());
    Assertions.assertEquals("counterparty_2", results.get(3).read(TestPayload.class).getMessage());
  }
}
