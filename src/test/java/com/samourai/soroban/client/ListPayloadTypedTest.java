package com.samourai.soroban.client;

import com.samourai.soroban.client.rpc.TestPayload;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListPayloadTypedTest extends AbstractTest {
  private static final Logger log = LoggerFactory.getLogger(ListPayloadTypedTest.class);

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  @Test
  public void distinctBySender() throws Exception {

    ListPayloadTyped listUntyped =
        new ListPayloadTyped(
            Arrays.asList(
                new SorobanPayloadTyped(new TestPayload("initiator_1"), paymentCodeInitiator),
                new SorobanPayloadTyped(new TestPayload("initiator_2"), paymentCodeInitiator),
                new SorobanPayloadTyped(new TestPayload("initiator_3"), paymentCodeInitiator),
                new SorobanPayloadTyped(new TestPayload("counterparty_1"), paymentCodeCounterparty),
                new SorobanPayloadTyped(
                    new TestPayload("counterparty_2"), paymentCodeCounterparty)));
    Assertions.assertEquals(5, listUntyped.size());

    listUntyped.distinctBySender();
    Assertions.assertEquals(2, listUntyped.size());

    List<? extends SorobanPayloadTyped> listDistinct = new LinkedList<>(listUntyped.getList());
    Assertions.assertEquals("initiator_3", listDistinct.get(0).getPayload());
    Assertions.assertEquals("counterparty_2", listDistinct.get(1).getPayload());
  }
}
