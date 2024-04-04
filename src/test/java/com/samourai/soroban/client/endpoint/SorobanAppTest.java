package com.samourai.soroban.client.endpoint;

import com.samourai.soroban.client.AbstractTest;
import com.samourai.wallet.bip47.rpc.Bip47Partner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SorobanAppTest extends AbstractTest {

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  @Test
  public void getDir() {
    Assertions.assertEquals("TESTNET/APP_TEST/" + appVersion + "/FOO", app.getDir("FOO"));
  }

  @Test
  public void getSharedDir() throws Exception {
    Bip47Partner bip47PartnerInitiator =
        rpcWalletInitiator.getBip47Partner(paymentCodeCounterparty, true);
    Bip47Partner bip47PartnerCounterparty =
        rpcWalletCounterparty.getBip47Partner(paymentCodeInitiator, false);

    String id = "test";
    Assertions.assertEquals(
        app.getDirShared(bip47PartnerInitiator, id),
        app.getDirShared(bip47PartnerCounterparty, id));
    Assertions.assertEquals(
        "TESTNET/APP_TEST/"
            + appVersion
            + "/SESSION/tb1qlsyqjg0uzrs2thcpmlrfavd037z8xx8uynjq69/test",
        app.getDirShared(bip47PartnerInitiator, id));
    Assertions.assertEquals(
        "TESTNET/APP_TEST/"
            + appVersion
            + "/SESSION/tb1qlsyqjg0uzrs2thcpmlrfavd037z8xx8uynjq69/test",
        app.getDirShared(bip47PartnerInitiator, id));

    id = "foo";
    Assertions.assertEquals(
        app.getDirShared(bip47PartnerInitiator, id),
        app.getDirShared(bip47PartnerCounterparty, id));
    Assertions.assertEquals(
        "TESTNET/APP_TEST/"
            + appVersion
            + "/SESSION/tb1qlsyqjg0uzrs2thcpmlrfavd037z8xx8uynjq69/foo",
        app.getDirShared(bip47PartnerInitiator, id));
    Assertions.assertEquals(
        "TESTNET/APP_TEST/"
            + appVersion
            + "/SESSION/tb1qlsyqjg0uzrs2thcpmlrfavd037z8xx8uynjq69/foo",
        app.getDirShared(bip47PartnerInitiator, id));
  }
}
