package com.samourai.soroban.client;

import com.samourai.http.client.IHttpClient;
import com.samourai.http.client.JavaHttpClient;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.cahoots.CahootsMessage;
import com.samourai.wallet.cahoots.CahootsService;
import com.samourai.wallet.cahoots.TestCahootsWallet;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.segwit.BIP84Wallet;
import com.samourai.wallet.soroban.client.SorobanMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanServiceTest extends AbstractTest {
  private static final Logger log = LoggerFactory.getLogger(SorobanServiceTest.class);

  private static final String SEED_WORDS = "all all all all all all all all all all all all";
  private static final String SEED_PASSPHRASE_INITIATOR = "initiator";
  private static final String SEED_PASSPHRASE_COUNTERPARTY = "counterparty";

  @Test
  public void stonewallx2() throws Exception {
    final BIP47Wallet bip47walletInitiator = bip47Wallet(SEED_WORDS, SEED_PASSPHRASE_INITIATOR);
    final BIP47Wallet bip47walletCounterparty =
        bip47Wallet(SEED_WORDS, SEED_PASSPHRASE_COUNTERPARTY);

    final PaymentCode paymentCodeInitiator = bip47Util.getPaymentCode(bip47walletInitiator);
    final PaymentCode paymentCodeCounterparty = bip47Util.getPaymentCode(bip47walletCounterparty);

    final int account = 0;
    final TestCahootsWallet cahootsWalletInitiator =
        computeCahootsWallet(SEED_WORDS, SEED_PASSPHRASE_INITIATOR);
    cahootsWalletInitiator.addUtxo(
        account, "senderTx1", 1, 10000, "tb1qkymumss6zj0rxy9l3v5vqxqwwffy8jjsyhrkrg");
    final TestCahootsWallet cahootsWalletCounterparty =
        computeCahootsWallet(SEED_WORDS, SEED_PASSPHRASE_COUNTERPARTY);
    cahootsWalletCounterparty.addUtxo(
        account, "counterpartyTx1", 1, 10000, "tb1qh287jqsh6mkpqmd8euumyfam00fkr78qhrdnde");

    // run initiator
    Thread threadInitiator =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                long amount = 5;
                String address = "tb1q9m8cc0jkjlc9zwvea5a2365u6px3yu646vgez4";

                // instanciate services
                CahootsService messageService = new CahootsService(params, cahootsWalletInitiator);
                IHttpClient httpClient = new JavaHttpClient(TIMEOUT_MS);
                SorobanService sorobanService =
                    new SorobanService(bip47Util, params, bip47walletInitiator, httpClient);

                try {
                  // run soroban as initiator
                  CahootsMessage message = messageService.newStonewallx2(account, amount, address);
                  SorobanMessage lastMessage =
                      sorobanService
                          .initiator(
                              account, messageService, paymentCodeCounterparty, TIMEOUT_MS, message)
                          .blockingLast();
                  Assertions.assertEquals("", lastMessage.toPayload());
                } catch (Exception e) {
                  setException(e);
                } finally {
                  try {
                    sorobanService.close();
                  } catch (Exception e) {
                  }
                }
              }
            });
    threadInitiator.start();

    // run contributor
    Thread threadContributor =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                // instanciate services
                CahootsService messageService =
                    new CahootsService(params, cahootsWalletCounterparty);
                IHttpClient httpClient = new JavaHttpClient(TIMEOUT_MS);
                SorobanService sorobanService =
                    new SorobanService(bip47Util, params, bip47walletCounterparty, httpClient);
                try {
                  // run soroban as counterparty
                  SorobanMessage lastMessage =
                      sorobanService
                          .contributor(account, messageService, paymentCodeInitiator, TIMEOUT_MS)
                          .blockingLast();
                  Assertions.assertEquals("", lastMessage.toPayload());
                } catch (Exception e) {
                  setException(e);
                } finally {
                  try {
                    sorobanService.close();
                  } catch (Exception e) {
                  }
                }
              }
            });
    threadContributor.start();

    assertNoException();
    threadInitiator.join();
    threadContributor.join();

    assertNoException();
    log.info("*** STONEWALLx2 SUCCESS ***");
  }

  private TestCahootsWallet computeCahootsWallet(String seedWords, String passphrase)
      throws Exception {
    byte[] seed = hdWalletFactory.computeSeedFromWords(seedWords);
    HD_Wallet bip84w = hdWalletFactory.getBIP84(seed, passphrase, params);
    BIP84Wallet bip84Wallet = new BIP84Wallet(bip84w, params);
    return new TestCahootsWallet(bip84Wallet, params);
  }
}
