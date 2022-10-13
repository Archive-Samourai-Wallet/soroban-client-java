package com.samourai.soroban.client.wallet;

import com.samourai.soroban.cahoots.CahootsContext;
import com.samourai.soroban.cahoots.Stonewallx2Context;
import com.samourai.soroban.cahoots.TypeInteraction;
import com.samourai.soroban.client.AbstractTest;
import com.samourai.soroban.client.OnlineSorobanInteraction;
import com.samourai.soroban.client.meeting.SorobanRequestMessage;
import com.samourai.soroban.client.meeting.SorobanResponseMessage;
import com.samourai.soroban.client.wallet.counterparty.CahootsSorobanCounterpartyListener;
import com.samourai.soroban.client.wallet.counterparty.SorobanCounterpartyListener;
import com.samourai.soroban.client.wallet.sender.CahootsSorobanInitiatorListener;
import com.samourai.soroban.client.wallet.sender.SorobanInitiatorListener;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.cahoots.Cahoots;
import com.samourai.wallet.cahoots.CahootsTestUtil;
import com.samourai.wallet.cahoots.CahootsType;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanWalletTest extends AbstractTest {
  private static final Logger log = LoggerFactory.getLogger(SorobanWalletTest.class);

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  @Test
  public void meetAndDecline() throws Exception {
    CahootsType cahootsType = CahootsType.STONEWALLX2;
    // run initiator
    Thread threadInitiator =
        new Thread(
            () -> {
              try {
                SorobanResponseMessage response =
                    asyncUtil.blockingGet(
                        sorobanWalletInitiator.meet(cahootsType, paymentCodeCounterparty));
                Assertions.assertEquals(false, response.isAccept());
              } catch (Exception e) {
                setException(e);
              }
            });
    threadInitiator.start();

    // run counterparty
    Thread threadCounterparty =
        new Thread(
            () -> {
              try {
                SorobanRequestMessage request =
                    asyncUtil.blockingGet(sorobanWalletCounterparty.receiveMeetingRequest());
                Assertions.assertEquals(cahootsType, request.getType());
                asyncUtil.blockingGet(sorobanWalletCounterparty.decline(request));
              } catch (Exception e) {
                setException(e);
              }
            });
    threadCounterparty.start();

    assertNoException();
    threadInitiator.join();
    threadCounterparty.join();

    assertNoException();
  }

  @Test
  public void stonewallx2_accept() throws Exception {

    final int account = 0;
    utxoProviderInitiator.addUtxo(
        account, "senderTx1", 1, 10000, "tb1qkymumss6zj0rxy9l3v5vqxqwwffy8jjsyhrkrg");
    utxoProviderCounterparty.addUtxo(
        account, "counterpartyTx1", 1, 10000, "tb1qh287jqsh6mkpqmd8euumyfam00fkr78qhrdnde");

    // run initiator
    Thread threadInitiator =
        new Thread(
            () -> {
              try {
                Cahoots cahoots = runInitiator(true, account, paymentCodeCounterparty);
                verify(
                    "{\"cahoots\":{\"fingerprint_collab\":\"f0d70870\",\"psbt\":\"\",\"destPaynym\":\"\",\"cpty_account\":0,\"spend_amount\":5,\"outpoints\":[{\"value\":10000,\"outpoint\":\"14cf9c6be92efcfe628aabd32b02c85e763615ddd430861bc18f6d366e4c4fd5-1\"},{\"value\":10000,\"outpoint\":\"9407b31fd0159dc4dd3f5377e3b18e4b4aafef2977a52e76b95c3f899cbb05ad-1\"}],\"type\":0,\"params\":\"testnet\",\"dest\":\"tb1q9m8cc0jkjlc9zwvea5a2365u6px3yu646vgez4\",\"version\":2,\"fee_amount\":284,\"fingerprint\":\"eed8a1cd\",\"step\":4,\"collabChange\":\"tb1qgzzjhah2q3pqfwpx5xpdrd649qmyl59a6m6cp4\",\"id\":\"testID\",\"account\":0,\"ts\":123456}}",
                    cahoots);
              } catch (Exception e) {
                setException(e);
              }
            });
    threadInitiator.start();

    // run counterparty
    Thread threadCounterparty =
        new Thread(
            () -> {
              try {
                Cahoots cahoots = runCounterparty(true, account);
                verify(
                    "{\"cahoots\":{\"fingerprint_collab\":\"f0d70870\",\"psbt\":\"\",\"destPaynym\":\"\",\"cpty_account\":0,\"spend_amount\":5,\"outpoints\":[{\"value\":10000,\"outpoint\":\"14cf9c6be92efcfe628aabd32b02c85e763615ddd430861bc18f6d366e4c4fd5-1\"},{\"value\":10000,\"outpoint\":\"9407b31fd0159dc4dd3f5377e3b18e4b4aafef2977a52e76b95c3f899cbb05ad-1\"}],\"type\":0,\"params\":\"testnet\",\"dest\":\"tb1q9m8cc0jkjlc9zwvea5a2365u6px3yu646vgez4\",\"version\":2,\"fee_amount\":284,\"fingerprint\":\"eed8a1cd\",\"step\":4,\"collabChange\":\"tb1qgzzjhah2q3pqfwpx5xpdrd649qmyl59a6m6cp4\",\"id\":\"testID\",\"account\":0,\"ts\":123456}}",
                    cahoots);
              } catch (Exception e) {
                setException(e);
              }
            });
    threadCounterparty.start();

    assertNoException();
    threadInitiator.join();
    threadCounterparty.join();

    assertNoException();
  }

  @Test
  public void stonewallx2_reject_counterparty() throws Exception {

    final int account = 0;
    utxoProviderInitiator.addUtxo(
        account, "senderTx1", 1, 10000, "tb1qkymumss6zj0rxy9l3v5vqxqwwffy8jjsyhrkrg");
    utxoProviderCounterparty.addUtxo(
        account, "counterpartyTx1", 1, 10000, "tb1qh287jqsh6mkpqmd8euumyfam00fkr78qhrdnde");

    // run initiator
    Thread threadInitiator =
        new Thread(
            () -> {
              try {
                runInitiator(true, account, paymentCodeCounterparty);
                Assertions.assertTrue(false);
              } catch (Exception e) {
                Assertions.assertEquals("Partner declined the Cahoots request", e.getMessage());
              }
            });
    threadInitiator.start();

    // run counterparty
    Thread threadCounterparty =
        new Thread(
            () -> {
              try {
                Assertions.assertNull(runCounterparty(false, account));
              } catch (Exception e) {
                setException(e);
              }
            });
    threadCounterparty.start();

    assertNoException();
    threadInitiator.join();
    threadCounterparty.join();

    assertNoException();
  }

  @Test
  public void stonewallx2_reject_initiator() throws Exception {

    final int account = 0;
    utxoProviderInitiator.addUtxo(
        account, "senderTx1", 1, 10000, "tb1qkymumss6zj0rxy9l3v5vqxqwwffy8jjsyhrkrg");
    utxoProviderCounterparty.addUtxo(
        account, "counterpartyTx1", 1, 10000, "tb1qh287jqsh6mkpqmd8euumyfam00fkr78qhrdnde");

    // run initiator
    Thread threadInitiator =
        new Thread(
            () -> {
              try {
                runInitiator(false, account, paymentCodeCounterparty);
                Assertions.assertTrue(false);
              } catch (Exception e) {
                Assertions.assertEquals("TEST_REJECT_BY_INITIATOR", e.getMessage());
              }
            });
    threadInitiator.start();

    // run counterparty
    Thread threadCounterparty =
        new Thread(
            () -> {
              try {
                Assertions.assertNull(runCounterparty(true, account));
                assertException("Partner failed with error: TEST_REJECT_BY_INITIATOR");
                setException(null);
              } catch (Exception e) {
                setException(e);
              }
            });
    threadCounterparty.start();

    assertNoException();
    threadInitiator.join();
    threadCounterparty.join();

    assertNoException();
  }

  private Cahoots runInitiator(
      final boolean ACCEPT, int account, PaymentCode paymentCodeCounterparty) throws Exception {
    // run soroban as initiator
    long amount = 5;
    String address = "tb1q9m8cc0jkjlc9zwvea5a2365u6px3yu646vgez4";

    CahootsContext cahootsContext =
        Stonewallx2Context.newInitiator(cahootsWalletInitiator, account, 1, amount, address, null);

    SorobanInitiatorListener initiatorListener =
        new CahootsSorobanInitiatorListener() {
          @Override
          public void onInteraction(OnlineSorobanInteraction interaction) throws Exception {
            try {
              Assertions.assertEquals(
                  TypeInteraction.TX_BROADCAST, interaction.getTypeInteraction());
              log.info("[INTERACTION] ==> TX_BROADCAST");
              if (ACCEPT) {
                interaction.sorobanAccept();
              } else {
                interaction.sorobanReject("TEST_REJECT_BY_INITIATOR");
              }
            } catch (Exception e) {
              setException(e);
            }
          }
        };
    return asyncUtil.blockingGet(
        sorobanWalletInitiator.meetAndInitiate(
            cahootsContext, paymentCodeCounterparty, initiatorListener));
  }

  private Cahoots runCounterparty(boolean ACCEPT, int account) throws Exception {
    // run soroban as counterparty
    Mutable<Cahoots> cahootsMutable = new MutableObject<>();
    SorobanCounterpartyListener listener =
        new CahootsSorobanCounterpartyListener(account) {
          @Override
          public void onRequest(SorobanRequestMessage request) {
            try {
              if (ACCEPT) {
                Cahoots cahoots =
                    asyncUtil.blockingGet(
                        sorobanWalletCounterparty.acceptAndCounterparty(request, this));
                cahootsMutable.setValue(cahoots);
              } else {
                asyncUtil.blockingGet(sorobanWalletCounterparty.decline(request));
              }
            } catch (Exception e) {
              setException(e);
            }
            sorobanWalletCounterparty.stopListening();
          }
        };
    sorobanWalletCounterparty.startListening(listener);
    while (sorobanWalletCounterparty.isListening()) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
    }
    return cahootsMutable.getValue();
  }

  protected void verify(String expectedPayload, Cahoots cahoots) throws Exception {
    CahootsTestUtil.cleanPayload(cahoots);
    String payloadStr = cahoots.toJSONString();
    Assertions.assertEquals(expectedPayload, payloadStr);
  }
}
