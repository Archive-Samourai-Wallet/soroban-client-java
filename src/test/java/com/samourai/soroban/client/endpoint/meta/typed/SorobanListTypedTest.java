package com.samourai.soroban.client.endpoint.meta.typed;

import com.samourai.soroban.client.AbstractTest;
import com.samourai.soroban.client.endpoint.meta.SorobanItemFilter;
import com.samourai.soroban.client.endpoint.meta.SorobanMetadata;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaNonce;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaSender;
import com.samourai.soroban.client.endpoint.wrapper.SorobanWrapper;
import com.samourai.soroban.client.rpc.RpcMode;
import com.samourai.soroban.client.rpc.TestPayload;
import com.samourai.soroban.client.rpc.TestResponsePayload;
import com.samourai.wallet.util.Pair;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanListTypedTest extends AbstractTest {
  private static final Logger log = LoggerFactory.getLogger(SorobanListTypedTest.class);

  private SorobanEndpointTyped endpoint;

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();

    TestPayload payloadInitiator1 = new TestPayload("initiator_1");
    TestPayload payloadInitiator2 = new TestPayload("initiator_2");
    TestPayload payloadInitiator2bis = new TestPayload("initiator_2");
    TestResponsePayload payloadInitiator3 = new TestResponsePayload("initiator_3_response");
    TestResponsePayload payloadInitiator4 = new TestResponsePayload("initiator_4_response");
    TestPayload payloadCounterparty1 = new TestPayload("counterparty_1");
    TestPayload payloadCounterparty2 = new TestPayload("counterparty_2");
    TestResponsePayload payloadCounterparty3 = new TestResponsePayload("counterparty_3_response");
    TestResponsePayload payloadCounterparty4 = new TestResponsePayload("counterparty_4_response");

    endpoint =
        new SorobanEndpointTyped(
            app.getDir("TEST"),
            RpcMode.SHORT,
            new SorobanWrapper[] {
              new SorobanWrapperMetaSender(), new SorobanWrapperMetaNonce(),
            });

    // send payloads
    rpcSessionInitiator.withSorobanClient(
        sorobanClient -> {
          asyncUtil.blockingAwait(endpoint.send(sorobanClient, payloadInitiator1));
          asyncUtil.blockingAwait(endpoint.send(sorobanClient, payloadInitiator2));
          asyncUtil.blockingAwait(endpoint.send(sorobanClient, payloadInitiator2bis));
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
    waitSorobanDelay();
  }

  @Test
  public void getList_unfiltered() throws Exception {
    // get all payloads
    List<SorobanItemTyped> list =
        asyncUtil.blockingGet(
            rpcSessionCounterparty.withSorobanClient(
                sorobanClient -> endpoint.getList(sorobanClient)));
    Assertions.assertEquals(9, list.size());
  }

  @Test
  public void getList_filtered() throws Exception {
    // filter by sender
    List<SorobanItemTyped> results = doTest(f -> f.filterBySender(paymentCodeCounterparty));
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
  public void filterLatestBySenderAndType() throws Exception {
    List<SorobanItemTyped> results =
        doTest(f -> f.filterByType(TestPayload.class).distinctBySenderWithLastNonce());
    Assertions.assertEquals(2, results.size());

    Assertions.assertEquals("initiator_2", results.get(0).read(TestPayload.class).getMessage());
    Assertions.assertEquals("counterparty_2", results.get(1).read(TestPayload.class).getMessage());
  }

  @Test
  public void filterLatestByUniqueIdAndType() throws Exception {
    List<SorobanItemTyped> results =
        doTest(f -> f.filterByType(TestPayload.class).distinctBySenderWithLastNonce());
    Assertions.assertEquals(2, results.size());

    Assertions.assertEquals("initiator_2", results.get(0).read(TestPayload.class).getMessage());
    Assertions.assertEquals("counterparty_2", results.get(1).read(TestPayload.class).getMessage());
  }

  @Test
  public void filterByType() throws Exception {
    List<SorobanItemTyped> results = doTest(f -> f.filterByType(TestPayload.class));
    Assertions.assertEquals(5, results.size());

    Assertions.assertEquals("initiator_1", results.get(0).read(TestPayload.class).getMessage());
    Assertions.assertEquals("initiator_2", results.get(1).read(TestPayload.class).getMessage());
    Assertions.assertEquals("initiator_2", results.get(2).read(TestPayload.class).getMessage());
    Assertions.assertEquals("counterparty_1", results.get(3).read(TestPayload.class).getMessage());
    Assertions.assertEquals("counterparty_2", results.get(4).read(TestPayload.class).getMessage());
  }

  @Test
  public void filterBySender() throws Exception {
    List<SorobanItemTyped> results = doTest(f -> f.filterBySender(paymentCodeInitiator));
    Assertions.assertEquals(5, results.size());

    Assertions.assertEquals("initiator_1", results.get(0).read(TestPayload.class).getMessage());
    Assertions.assertEquals("initiator_2", results.get(1).read(TestPayload.class).getMessage());
    Assertions.assertEquals("initiator_2", results.get(2).read(TestPayload.class).getMessage());
    Assertions.assertEquals(
        "initiator_3_response",
        results.get(3).read(TestResponsePayload.class).getResponseMessage());
    Assertions.assertEquals(
        "initiator_4_response",
        results.get(4).read(TestResponsePayload.class).getResponseMessage());
  }

  @Test
  public void filterByObject() throws Exception {
    List<TestPayload> results =
        doTestTyped(
            TestPayload.class,
            f ->
                f.filterByObject(
                    TestPayload.class,
                    (i, o) ->
                        i.getMetaSender().equals(paymentCodeInitiator)
                            && o.getMessage().endsWith("_2")));
    Assertions.assertEquals(2, results.size());
    Assertions.assertEquals("initiator_2", results.get(0).getMessage());
    Assertions.assertEquals("initiator_2", results.get(1).getMessage());
  }

  @Test
  public void getListObjects() throws Exception {
    List<TestPayload> results =
        asyncUtil.blockingGet(
            rpcSessionCounterparty.withSorobanClient(
                sorobanClient ->
                    endpoint.getListObjects(
                        sorobanClient,
                        TestPayload.class,
                        f -> f.filterBySender(paymentCodeInitiator))));
    Assertions.assertEquals(3, results.size());
    Assertions.assertEquals("initiator_1", results.get(0).getMessage());
    Assertions.assertEquals("initiator_2", results.get(1).getMessage());
    Assertions.assertEquals("initiator_2", results.get(2).getMessage());
  }

  @Test
  public void getListObjectsWithMetadata() throws Exception {
    List<Pair<TestPayload, SorobanMetadata>> results =
        asyncUtil.blockingGet(
            rpcSessionCounterparty.withSorobanClient(
                sorobanClient ->
                    endpoint.getListObjectsWithMetadata(
                        sorobanClient,
                        TestPayload.class,
                        f -> f.filterBySender(paymentCodeInitiator))));

    Assertions.assertEquals(3, results.size());
    Assertions.assertEquals("initiator_1", results.get(0).getLeft().getMessage());
    Assertions.assertEquals(
        SorobanWrapperMetaSender.getSender(results.get(0).getRight()), paymentCodeInitiator);
    Assertions.assertEquals("initiator_2", results.get(1).getLeft().getMessage());
    Assertions.assertEquals(
        SorobanWrapperMetaSender.getSender(results.get(1).getRight()), paymentCodeInitiator);
    Assertions.assertEquals("initiator_2", results.get(2).getLeft().getMessage());
    Assertions.assertEquals(
        SorobanWrapperMetaSender.getSender(results.get(2).getRight()), paymentCodeInitiator);
  }

  protected List<SorobanItemTyped> doTest(
      Consumer<SorobanItemFilter<SorobanItemTyped>> filterBuilder) throws Exception {
    return asyncUtil.blockingGet(
        rpcSessionCounterparty.withSorobanClient(
            sorobanClient -> endpoint.getList(sorobanClient, filterBuilder)));
  }

  protected <T> List<T> doTestTyped(
      Class<T> type, Consumer<SorobanItemFilter<SorobanItemTyped>> filterBuilder) throws Exception {
    return asyncUtil.blockingGet(
        rpcSessionCounterparty.withSorobanClient(
            sorobanClient -> endpoint.getListObjects(sorobanClient, type, filterBuilder)));
  }
}
