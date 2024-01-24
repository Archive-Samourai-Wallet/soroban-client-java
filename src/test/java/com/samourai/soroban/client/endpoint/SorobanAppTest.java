package com.samourai.soroban.client.endpoint;

import com.samourai.soroban.client.AbstractTest;
import com.samourai.soroban.client.endpoint.string.SorobanEndpointString;
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
    SorobanEndpointString endpoint = new SorobanEndpointString(app, "FOO", RpcMode.SHORT);
    Assertions.assertEquals("TESTNET/HELLO_WORLD/" + appVersion + "/FOO", endpoint.getDir());
  }

  /*
  @Test
  public void send_get_object() throws Exception {
    TestPayload payload = new TestPayload("HELLO WORLD");

    // send payload
    rpcSessionInitiator.withSorobanClient(
            sorobanClient -> {
              asyncUtil.blockingAwait(app.getEndpointHello().send(sorobanClient, payload));
              return null;
            });

    // get payload
    TestPayload resultPayload =
            rpcSessionCounterparty.withSorobanClient(
                    sorobanClient -> {
                      return asyncUtil.blockingGet(app.getEndpointHello().getNext(sorobanClient))
                              .read(TestPayload.class);
                    });
    Assertions.assertEquals(payload.getMessage(), resultPayload.getMessage());
  }*/

  @Test
  public void getSharedDir() throws Exception {
    Bip47PartnerImpl bip47PartnerInitiator =
        new Bip47PartnerImpl(
            bip47WalletInitiator, paymentCodeCounterparty, true, cryptoUtil, bip47Util);
    Bip47PartnerImpl bip47PartnerCounterparty =
        new Bip47PartnerImpl(
            bip47WalletCounterparty, paymentCodeInitiator, false, cryptoUtil, bip47Util);

    String id = "test";
    Assertions.assertEquals(
        app.getDirShared(bip47PartnerInitiator, id),
        app.getDirShared(bip47PartnerCounterparty, id));
    Assertions.assertEquals(
        "TESTNET/HELLO_WORLD/"
            + appVersion
            + "/SESSION/tb1qlsyqjg0uzrs2thcpmlrfavd037z8xx8uynjq69/test",
        app.getDirShared(bip47PartnerInitiator, id));
    Assertions.assertEquals(
        "TESTNET/HELLO_WORLD/"
            + appVersion
            + "/SESSION/tb1qlsyqjg0uzrs2thcpmlrfavd037z8xx8uynjq69/test",
        app.getDirSharedClear(bip47PartnerInitiator, id));

    id = "foo";
    Assertions.assertEquals(
        app.getDirShared(bip47PartnerInitiator, id),
        app.getDirShared(bip47PartnerCounterparty, id));
    Assertions.assertEquals(
        "TESTNET/HELLO_WORLD/"
            + appVersion
            + "/SESSION/tb1qlsyqjg0uzrs2thcpmlrfavd037z8xx8uynjq69/foo",
        app.getDirShared(bip47PartnerInitiator, id));
    Assertions.assertEquals(
        "TESTNET/HELLO_WORLD/"
            + appVersion
            + "/SESSION/tb1qlsyqjg0uzrs2thcpmlrfavd037z8xx8uynjq69/foo",
        app.getDirSharedClear(bip47PartnerInitiator, id));
  }
}
