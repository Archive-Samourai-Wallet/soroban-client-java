package com.samourai.soroban.client.cahoots;

import com.samourai.soroban.cahoots.CahootsContext;
import com.samourai.soroban.cahoots.TypeInteraction;
import com.samourai.soroban.client.AbstractTest;
import com.samourai.soroban.client.OnlineSorobanInteraction;
import com.samourai.soroban.client.SorobanMessage;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import io.reactivex.functions.Consumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanCahootsServiceTest extends AbstractTest {
  private static final Logger log = LoggerFactory.getLogger(SorobanCahootsServiceTest.class);

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  @Test
  public void stonewallx2() throws Exception {

    final int account = 0;
    utxoProviderInitiator.addUtxo(
        account, "senderTx1", 1, 10000, "tb1qkymumss6zj0rxy9l3v5vqxqwwffy8jjsyhrkrg");
    utxoProviderCounterparty.addUtxo(
        account, "counterpartyTx1", 1, 10000, "tb1qh287jqsh6mkpqmd8euumyfam00fkr78qhrdnde");

    // run initiator
    Thread threadInitiator =
        new Thread(
            () -> {
              /*
               * #1 => accept
               */
              runInitiator(true, account, paymentCodeCounterparty);

              /*
               * #2 => reject
               */
              runInitiator(false, account, paymentCodeCounterparty);
            });
    threadInitiator.start();

    // run contributor
    Thread threadContributor =
        new Thread(
            () -> {
              /** #1 => accept */
              runContributor(true, account, paymentCodeInitiator);

              /** #2 => reject */
              runContributor(false, account, paymentCodeInitiator);
            });
    threadContributor.start();

    assertNoException();
    threadInitiator.join();
    threadContributor.join();

    assertNoException();
  }

  private void runInitiator(
      final boolean ACCEPT, int account, PaymentCode paymentCodeCounterparty) {
    // run soroban as initiator
    long amount = 5;
    String address = "tb1q9m8cc0jkjlc9zwvea5a2365u6px3yu646vgez4";

    try {
      CahootsContext cahootsContext =
          CahootsContext.newInitiatorStonewallx2(cahootsWalletInitiator, account, amount, address);

      Consumer<OnlineSorobanInteraction> onInteraction =
          interaction -> {
            Assertions.assertEquals(TypeInteraction.TX_BROADCAST, interaction.getTypeInteraction());
            log.info("[INTERACTION] ==> TX_BROADCAST");
            if (ACCEPT) {
              interaction.sorobanAccept();
            } else {
              interaction.sorobanReject("TEST_REJECT");
            }
          };
      SorobanMessage lastMessage =
          sorobanCahootsService
              .initiator(cahootsContext, paymentCodeCounterparty, TIMEOUT_MS, onInteraction)
              .blockingLast();
      if (ACCEPT) {
        verify(
            "{\"cahoots\":\"{\\\"cahoots\\\":{\\\"fingerprint_collab\\\":\\\"f0d70870\\\",\\\"psbt\\\":\\\"\\\",\\\"cpty_account\\\":0,\\\"spend_amount\\\":5,\\\"outpoints\\\":[{\\\"value\\\":10000,\\\"outpoint\\\":\\\"14cf9c6be92efcfe628aabd32b02c85e763615ddd430861bc18f6d366e4c4fd5-1\\\"},{\\\"value\\\":10000,\\\"outpoint\\\":\\\"9407b31fd0159dc4dd3f5377e3b18e4b4aafef2977a52e76b95c3f899cbb05ad-1\\\"}],\\\"type\\\":0,\\\"params\\\":\\\"testnet\\\",\\\"dest\\\":\\\"tb1q9m8cc0jkjlc9zwvea5a2365u6px3yu646vgez4\\\",\\\"version\\\":2,\\\"fee_amount\\\":314,\\\"fingerprint\\\":\\\"eed8a1cd\\\",\\\"step\\\":4,\\\"collabChange\\\":\\\"tb1qv4ak4l0w76qflk4uulavu22kxtaajnltkzxyq5\\\",\\\"id\\\":\\\"testID\\\",\\\"account\\\":0,\\\"ts\\\":123456}}\"}",
            lastMessage);
      } else {
        Assertions.assertTrue(false);
      }
    } catch (Exception e) {
      if (ACCEPT) {
        setException(e);
      } else {
        Assertions.assertTrue(e.getMessage().contains("TEST_REJECT"));
      }
    }
  }

  private void runContributor(boolean ACCEPT, int account, PaymentCode paymentCodeInitiator) {
    try {
      // run soroban as counterparty
      CahootsContext cahootsContext =
          CahootsContext.newCounterpartyStonewallx2(cahootsWalletCounterparty, account);
      SorobanMessage lastMessage =
          sorobanCahootsService
              .contributor(cahootsContext, paymentCodeInitiator, TIMEOUT_MS)
              .blockingLast();
      if (ACCEPT) {
        verify(
            "{\"cahoots\":\"{\\\"cahoots\\\":{\\\"fingerprint_collab\\\":\\\"f0d70870\\\",\\\"psbt\\\":\\\"\\\",\\\"cpty_account\\\":0,\\\"spend_amount\\\":5,\\\"outpoints\\\":[{\\\"value\\\":10000,\\\"outpoint\\\":\\\"14cf9c6be92efcfe628aabd32b02c85e763615ddd430861bc18f6d366e4c4fd5-1\\\"},{\\\"value\\\":10000,\\\"outpoint\\\":\\\"9407b31fd0159dc4dd3f5377e3b18e4b4aafef2977a52e76b95c3f899cbb05ad-1\\\"}],\\\"type\\\":0,\\\"params\\\":\\\"testnet\\\",\\\"dest\\\":\\\"tb1q9m8cc0jkjlc9zwvea5a2365u6px3yu646vgez4\\\",\\\"version\\\":2,\\\"fee_amount\\\":314,\\\"fingerprint\\\":\\\"eed8a1cd\\\",\\\"step\\\":4,\\\"collabChange\\\":\\\"tb1qv4ak4l0w76qflk4uulavu22kxtaajnltkzxyq5\\\",\\\"id\\\":\\\"testID\\\",\\\"account\\\":0,\\\"ts\\\":123456}}\"}",
            lastMessage);
      } else {
        Assertions.assertTrue(false);
      }
    } catch (Exception e) {
      if (ACCEPT) {
        setException(e);
      } else {
        Assertions.assertTrue(e.getMessage().contains("TEST_REJECT"));
      }
    }
  }
}
