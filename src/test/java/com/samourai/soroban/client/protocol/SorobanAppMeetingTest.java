package com.samourai.soroban.client.protocol;

import com.samourai.soroban.client.AbstractTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanAppMeetingTest extends AbstractTest {
  private static final Logger log = LoggerFactory.getLogger(SorobanAppMeetingTest.class);

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  @Test
  public void getMeeetingAddressReceive() throws Exception {
    String addressReceive =
        sorobanConfig
            .getSorobanWalletService()
            .getSorobanProtocol()
            .getMeeetingAddressReceive(
                rpcWalletInitiator.getBip47Account(),
                cahootsWalletCounterparty.getBip47Account().getPaymentCode());
    Assertions.assertEquals("tb1q2s8kr83fkxc65q9axmhk0mfmqn6astjsn0fzzd", addressReceive);
  }

  @Test
  public void getMeeetingAddressSend() throws Exception {
    String addressSend =
        sorobanConfig
            .getSorobanWalletService()
            .getSorobanProtocol()
            .getMeeetingAddressSend(
                rpcWalletCounterparty.getBip47Account(),
                cahootsWalletInitiator.getBip47Account().getPaymentCode());
    Assertions.assertEquals("tb1q2s8kr83fkxc65q9axmhk0mfmqn6astjsn0fzzd", addressSend);
  }
}
