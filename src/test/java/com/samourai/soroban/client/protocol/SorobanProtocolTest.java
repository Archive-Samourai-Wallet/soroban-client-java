package com.samourai.soroban.client.protocol;

import com.samourai.soroban.client.AbstractTest;
import com.samourai.wallet.bip47.rpc.Bip47PartnerImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SorobanProtocolTest extends AbstractTest {
  private SorobanProtocol sorobanProtocol;

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();
    sorobanProtocol = new SorobanProtocol(whirlpoolNetwork, "DEMO", "1.0");
  }

  @Test
  public void getDir() {
    String id = "test";
    Assertions.assertEquals("TESTNET/DEMO/1.0/test", sorobanProtocol.getDir(id));
    Assertions.assertEquals("TESTNET/DEMO/1.0/test", sorobanProtocol.getDirClear(id));

    id = "foo";
    Assertions.assertEquals("TESTNET/DEMO/1.0/foo", sorobanProtocol.getDir(id));
    Assertions.assertEquals("TESTNET/DEMO/1.0/foo", sorobanProtocol.getDirClear(id));
  }

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
        sorobanProtocol.getDirShared(bip47PartnerInitiator, id),
        sorobanProtocol.getDirShared(bip47PartnerCounterparty, id));
    Assertions.assertEquals(
        "TESTNET/DEMO/1.0/SESSION/tb1qlsyqjg0uzrs2thcpmlrfavd037z8xx8uynjq69/test",
        sorobanProtocol.getDirShared(bip47PartnerInitiator, id));
    Assertions.assertEquals(
        "TESTNET/DEMO/1.0/tb1qlsyqjg0uzrs2thcpmlrfavd037z8xx8uynjq69/test",
        sorobanProtocol.getDirSharedClear(bip47PartnerInitiator, id));

    id = "foo";
    Assertions.assertEquals(
        sorobanProtocol.getDirShared(bip47PartnerInitiator, id),
        sorobanProtocol.getDirShared(bip47PartnerCounterparty, id));
    Assertions.assertEquals(
        "TESTNET/DEMO/1.0/tb1qlsyqjg0uzrs2thcpmlrfavd037z8xx8uynjq69/foo",
        sorobanProtocol.getDirShared(bip47PartnerInitiator, id));
    Assertions.assertEquals(
        "TESTNET/DEMO/1.0/tb1qlsyqjg0uzrs2thcpmlrfavd037z8xx8uynjq69/foo",
        sorobanProtocol.getDirSharedClear(bip47PartnerInitiator, id));
  }
}
