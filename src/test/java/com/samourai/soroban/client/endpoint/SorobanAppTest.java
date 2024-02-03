package com.samourai.soroban.client.endpoint;

import com.samourai.soroban.client.AbstractTest;
import com.samourai.soroban.client.endpoint.wrapper.SorobanWrapperString;
import com.samourai.soroban.client.rpc.RpcMode;
import com.samourai.wallet.bip47.rpc.Bip47PartnerImpl;
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
    SorobanEndpointRaw endpoint =
        new SorobanEndpointRaw(app, "FOO", RpcMode.SHORT, new SorobanWrapperString[] {});
    Assertions.assertEquals("TESTNET/APP_TEST/" + appVersion + "/FOO", endpoint.getDir());
  }

  @Test
  public void getSharedDir() throws Exception {
    Bip47PartnerImpl bip47PartnerInitiator =
        new Bip47PartnerImpl(
            bip47AccountInitiator, paymentCodeCounterparty, true, cryptoUtil, bip47Util);
    Bip47PartnerImpl bip47PartnerCounterparty =
        new Bip47PartnerImpl(
            bip47AccountCounterparty, paymentCodeInitiator, false, cryptoUtil, bip47Util);

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
        app.getDirSharedClear(bip47PartnerInitiator, id));

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
        app.getDirSharedClear(bip47PartnerInitiator, id));
  }
}
