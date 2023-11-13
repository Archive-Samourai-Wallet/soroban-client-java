package com.samourai.soroban.client;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListUntypedPayloadWithSenderTest extends AbstractTest {
  private static final Logger log = LoggerFactory.getLogger(ListUntypedPayloadWithSenderTest.class);

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  @Test
  public void distinctBySender() throws Exception {

    ListUntypedPayloadWithSender listUntyped =
        new ListUntypedPayloadWithSender(
            Arrays.asList(
                new UntypedPayloadWithSender("initiator_1", paymentCodeInitiator),
                new UntypedPayloadWithSender("initiator_2", paymentCodeInitiator),
                new UntypedPayloadWithSender("initiator_3", paymentCodeInitiator),
                new UntypedPayloadWithSender("counterparty_1", paymentCodeCounterparty),
                new UntypedPayloadWithSender("counterparty_2", paymentCodeCounterparty)));
    Assertions.assertEquals(5, listUntyped.size());

    listUntyped.distinctBySender();
    Assertions.assertEquals(2, listUntyped.size());

    List<? extends UntypedPayloadWithSender> listDistinct = new LinkedList<>(listUntyped.getList());
    Assertions.assertEquals("initiator_3", listDistinct.get(0).getPayload());
    Assertions.assertEquals("counterparty_2", listDistinct.get(1).getPayload());
  }
}
