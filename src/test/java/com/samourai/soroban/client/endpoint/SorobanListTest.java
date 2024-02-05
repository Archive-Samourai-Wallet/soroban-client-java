package com.samourai.soroban.client.endpoint;

import com.samourai.soroban.client.AbstractTest;
import com.samourai.soroban.client.endpoint.meta.SorobanItemFilter;
import com.samourai.soroban.client.endpoint.meta.typed.SorobanEndpointTyped;
import com.samourai.soroban.client.endpoint.meta.typed.SorobanItemTyped;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaNonce;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaSender;
import com.samourai.soroban.client.endpoint.wrapper.SorobanWrapper;
import com.samourai.soroban.client.rpc.RpcMode;
import com.samourai.soroban.client.rpc.TestPayload;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanListTest extends AbstractTest {
  private static final Logger log = LoggerFactory.getLogger(SorobanListTest.class);

  private SorobanEndpointTyped endpoint;

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();

    TestPayload payloadInitiator1 = new TestPayload("initiator_1");
    TestPayload payloadInitiator2 = new TestPayload("initiator_2");
    TestPayload payloadCounterparty1 = new TestPayload("counterparty_1");
    TestPayload payloadCounterparty2 = new TestPayload("counterparty_2");

    endpoint =
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
          return null;
        });
    rpcSessionCounterparty.withSorobanClient(
        sorobanClient -> {
          asyncUtil.blockingAwait(endpoint.send(sorobanClient, payloadCounterparty1));
          asyncUtil.blockingAwait(endpoint.send(sorobanClient, payloadCounterparty2));
          return null;
        });

    // get all payloads
    List<SorobanItemTyped> list =
        asyncUtil.blockingGet(
            rpcSessionCounterparty.withSorobanClient(
                sorobanClient -> endpoint.getList(sorobanClient)));
    Assertions.assertEquals(4, list.size());
  }

  @Test
  public void filterBySender() throws Exception {
    List<SorobanItemTyped> results = doTest(f -> f.filterBySender(paymentCodeCounterparty));
    Assertions.assertEquals(2, results.size());
  }

  @Test
  public void distinctBySenderWithLastNonce() throws Exception {
    List<SorobanItemTyped> results = doTest(f -> f.distinctBySenderWithLastNonce());
    Assertions.assertEquals(2, results.size());

    Assertions.assertEquals("initiator_2", results.get(0).read(TestPayload.class).getMessage());
    Assertions.assertEquals("counterparty_2", results.get(1).read(TestPayload.class).getMessage());
  }

  @Test
  public void sortByNonceAsc() throws Exception {
    List<SorobanItemTyped> results = doTest(f -> f.sortByNonce(false));
    Assertions.assertEquals(4, results.size());

    Assertions.assertEquals("initiator_1", results.get(0).read(TestPayload.class).getMessage());
    Assertions.assertEquals("initiator_2", results.get(1).read(TestPayload.class).getMessage());
    Assertions.assertEquals("counterparty_1", results.get(2).read(TestPayload.class).getMessage());
    Assertions.assertEquals("counterparty_2", results.get(3).read(TestPayload.class).getMessage());
  }

  @Test
  public void sortByNonceDesc() throws Exception {
    List<SorobanItemTyped> results = doTest(f -> f.sortByNonce(true));
    Assertions.assertEquals(4, results.size());

    Assertions.assertEquals("counterparty_2", results.get(0).read(TestPayload.class).getMessage());
    Assertions.assertEquals("counterparty_1", results.get(1).read(TestPayload.class).getMessage());
    Assertions.assertEquals("initiator_2", results.get(2).read(TestPayload.class).getMessage());
    Assertions.assertEquals("initiator_1", results.get(3).read(TestPayload.class).getMessage());
  }

  protected List<SorobanItemTyped> doTest(
      Consumer<SorobanItemFilter<SorobanItemTyped>> filterBuilder) throws Exception {
    return asyncUtil.blockingGet(
        rpcSessionCounterparty.withSorobanClient(
            sorobanClient -> endpoint.getList(sorobanClient, filterBuilder)));
  }
}
