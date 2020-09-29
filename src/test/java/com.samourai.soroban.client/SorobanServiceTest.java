package com.samourai.soroban.client;

import com.samourai.http.client.IHttpClient;
import com.samourai.http.client.JavaHttpClient;
import com.samourai.soroban.client.cahoots.SorobanCahootsService;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.cahoots.TestCahootsWallet;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.segwit.BIP84Wallet;
import com.samourai.wallet.soroban.cahoots.CahootsContext;
import com.samourai.wallet.soroban.cahoots.TypeInteraction;
import com.samourai.wallet.soroban.client.SorobanInteraction;
import com.samourai.wallet.soroban.client.SorobanMessage;
import io.reactivex.functions.Consumer;
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
                // instanciate services
                IHttpClient httpClient = new JavaHttpClient(TIMEOUT_MS);
                final SorobanCahootsService sorobanCahootsService =
                    new SorobanCahootsService(
                        bip47Util, PROVIDER_JAVA, cahootsWalletInitiator, httpClient);

                try {
                  // run soroban as initiator
                  long amount = 5;
                  String address = "tb1q9m8cc0jkjlc9zwvea5a2365u6px3yu646vgez4";
                  CahootsContext cahootsContext =
                      CahootsContext.newInitiatorStonewallx2(amount, address);

                  sorobanCahootsService
                      .getSorobanService()
                      .getOnInteraction()
                      .subscribe(
                          new Consumer<SorobanInteraction>() {
                            @Override
                            public void accept(SorobanInteraction interaction) throws Exception {
                              Assertions.assertEquals(
                                  TypeInteraction.TX_BROADCAST, interaction.getTypeInteraction());
                              log.info("[INTERACTION] ==> TX_BROADCAST");
                              interaction.accept();
                            }
                          });
                  SorobanMessage lastMessage =
                      sorobanCahootsService
                          .initiator(account, cahootsContext, paymentCodeCounterparty, TIMEOUT_MS)
                          .blockingLast();
                  verify(
                      "{\"cahoots\":\"{\\\"cahoots\\\":{\\\"fingerprint_collab\\\":\\\"f0d70870\\\",\\\"psbt\\\":\\\"\\\",\\\"cpty_account\\\":0,\\\"spend_amount\\\":5,\\\"outpoints\\\":[{\\\"value\\\":10000,\\\"outpoint\\\":\\\"14cf9c6be92efcfe628aabd32b02c85e763615ddd430861bc18f6d366e4c4fd5-1\\\"},{\\\"value\\\":10000,\\\"outpoint\\\":\\\"9407b31fd0159dc4dd3f5377e3b18e4b4aafef2977a52e76b95c3f899cbb05ad-1\\\"}],\\\"type\\\":0,\\\"dest\\\":\\\"tb1q9m8cc0jkjlc9zwvea5a2365u6px3yu646vgez4\\\",\\\"params\\\":\\\"testnet\\\",\\\"version\\\":2,\\\"fee_amount\\\":314,\\\"fingerprint\\\":\\\"eed8a1cd\\\",\\\"step\\\":4,\\\"collabChange\\\":\\\"tb1qv4ak4l0w76qflk4uulavu22kxtaajnltkzxyq5\\\",\\\"id\\\":\\\"testID\\\",\\\"account\\\":0,\\\"ts\\\":123456}}\"}",
                      lastMessage);
                } catch (Exception e) {
                  setException(e);
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
                IHttpClient httpClient = new JavaHttpClient(TIMEOUT_MS);
                SorobanCahootsService sorobanCahootsService =
                    new SorobanCahootsService(
                        bip47Util, PROVIDER_JAVA, cahootsWalletCounterparty, httpClient);

                try {
                  // run soroban as counterparty
                  CahootsContext cahootsContext = CahootsContext.newCounterpartyStonewallx2();
                  SorobanMessage lastMessage =
                      sorobanCahootsService
                          .contributor(account, cahootsContext, paymentCodeInitiator, TIMEOUT_MS)
                          .blockingLast();
                  verify(
                      "{\"cahoots\":\"{\\\"cahoots\\\":{\\\"fingerprint_collab\\\":\\\"f0d70870\\\",\\\"psbt\\\":\\\"\\\",\\\"cpty_account\\\":0,\\\"spend_amount\\\":5,\\\"outpoints\\\":[{\\\"value\\\":10000,\\\"outpoint\\\":\\\"14cf9c6be92efcfe628aabd32b02c85e763615ddd430861bc18f6d366e4c4fd5-1\\\"},{\\\"value\\\":10000,\\\"outpoint\\\":\\\"9407b31fd0159dc4dd3f5377e3b18e4b4aafef2977a52e76b95c3f899cbb05ad-1\\\"}],\\\"type\\\":0,\\\"dest\\\":\\\"tb1q9m8cc0jkjlc9zwvea5a2365u6px3yu646vgez4\\\",\\\"params\\\":\\\"testnet\\\",\\\"version\\\":2,\\\"fee_amount\\\":314,\\\"fingerprint\\\":\\\"eed8a1cd\\\",\\\"step\\\":4,\\\"collabChange\\\":\\\"tb1qv4ak4l0w76qflk4uulavu22kxtaajnltkzxyq5\\\",\\\"id\\\":\\\"testID\\\",\\\"account\\\":0,\\\"ts\\\":123456}}\"}",
                      lastMessage);
                } catch (Exception e) {
                  setException(e);
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
