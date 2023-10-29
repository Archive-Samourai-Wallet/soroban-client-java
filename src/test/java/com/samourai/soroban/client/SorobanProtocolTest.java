package com.samourai.soroban.client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanProtocolTest extends AbstractTest {
  private static final Logger log = LoggerFactory.getLogger(SorobanProtocolTest.class);

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  @Test
  public void getMeeetingAddressReceive() throws Exception {
    String addressReceive =
        sorobanProtocol.getMeeetingAddressReceive(
            cahootsWalletInitiator.getRpcWallet(),
            cahootsWalletCounterparty.getPaymentCode(),
            params,
            bip47Util);
    Assertions.assertEquals("tb1q2s8kr83fkxc65q9axmhk0mfmqn6astjsn0fzzd", addressReceive);
  }

  @Test
  public void getMeeetingAddressSend() throws Exception {
    String addressSend =
        sorobanProtocol.getMeeetingAddressSend(
            cahootsWalletCounterparty.getRpcWallet(),
            cahootsWalletInitiator.getPaymentCode(),
            params,
            bip47Util);
    Assertions.assertEquals("tb1q2s8kr83fkxc65q9axmhk0mfmqn6astjsn0fzzd", addressSend);
  }
}