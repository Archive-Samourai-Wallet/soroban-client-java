package com.samourai.soroban.client;

import com.samourai.http.client.IHttpClient;
import com.samourai.http.client.JavaHttpClient;
import com.samourai.soroban.client.cahoots.CahootsInteraction;
import com.samourai.soroban.client.cahoots.OnlineCahootsInteraction;
import com.samourai.soroban.client.cahoots.OnlineCahootsMessage;
import com.samourai.soroban.client.cahoots.OnlineCahootsService;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.cahoots.TestCahootsWallet;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.segwit.BIP84Wallet;
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
                long amount = 5;
                String address = "tb1q9m8cc0jkjlc9zwvea5a2365u6px3yu646vgez4";

                // instanciate services
                final OnlineCahootsService messageService =
                    new OnlineCahootsService(params, cahootsWalletInitiator);
                IHttpClient httpClient = new JavaHttpClient(TIMEOUT_MS);
                final SorobanService sorobanService =
                    new SorobanService(
                        bip47Util, params, PROVIDER_JAVA, bip47walletInitiator, httpClient);

                try {
                  // run soroban as initiator
                  OnlineCahootsMessage message =
                      messageService.newStonewallx2(account, amount, address);
                  sorobanService
                      .getOnInteraction()
                      .subscribe(
                          new Consumer<SorobanMessage>() {
                            @Override
                            public void accept(SorobanMessage message) throws Exception {
                              OnlineCahootsInteraction interactiveMessage =
                                  (OnlineCahootsInteraction) message;
                              Assertions.assertEquals(
                                  CahootsInteraction.TX_BROADCAST,
                                  interactiveMessage.getInteraction());
                              log.info("[INTERACTION] ==> TX_BROADCAST");
                              SorobanMessage confirmMessage =
                                  messageService.confirmTxBroadcast(message);
                              sorobanService.replyInteractive(confirmMessage);
                            }
                          });
                  SorobanMessage lastMessage =
                      sorobanService
                          .initiator(
                              account, messageService, paymentCodeCounterparty, TIMEOUT_MS, message)
                          .blockingLast();
                  ;
                  verify(
                      "{\"cahoots\":\"{\\\"cahoots\\\":{\\\"fingerprint_collab\\\":\\\"f0d70870\\\",\\\"psbt\\\":\\\"\\\",\\\"cpty_account\\\":0,\\\"spend_amount\\\":5,\\\"outpoints\\\":[{\\\"value\\\":10000,\\\"outpoint\\\":\\\"14cf9c6be92efcfe628aabd32b02c85e763615ddd430861bc18f6d366e4c4fd5-1\\\"},{\\\"value\\\":10000,\\\"outpoint\\\":\\\"9407b31fd0159dc4dd3f5377e3b18e4b4aafef2977a52e76b95c3f899cbb05ad-1\\\"}],\\\"type\\\":0,\\\"dest\\\":\\\"tb1q9m8cc0jkjlc9zwvea5a2365u6px3yu646vgez4\\\",\\\"params\\\":\\\"testnet\\\",\\\"version\\\":2,\\\"fee_amount\\\":314,\\\"fingerprint\\\":\\\"eed8a1cd\\\",\\\"step\\\":4,\\\"collabChange\\\":\\\"tb1qv4ak4l0w76qflk4uulavu22kxtaajnltkzxyq5\\\",\\\"id\\\":\\\"testID\\\",\\\"account\\\":0,\\\"ts\\\":123456}}\",\"done\":true}",
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
                OnlineCahootsService messageService =
                    new OnlineCahootsService(params, cahootsWalletCounterparty);
                IHttpClient httpClient = new JavaHttpClient(TIMEOUT_MS);
                SorobanService sorobanService =
                    new SorobanService(
                        bip47Util, params, PROVIDER_JAVA, bip47walletCounterparty, httpClient);
                try {
                  // run soroban as counterparty
                  SorobanMessage lastMessage =
                      sorobanService
                          .contributor(account, messageService, paymentCodeInitiator, TIMEOUT_MS)
                          .blockingLast();
                  verify(
                      "{\"cahoots\":\"{\\\"cahoots\\\":{\\\"fingerprint_collab\\\":\\\"f0d70870\\\",\\\"psbt\\\":\\\"\\\",\\\"cpty_account\\\":0,\\\"spend_amount\\\":5,\\\"outpoints\\\":[{\\\"value\\\":10000,\\\"outpoint\\\":\\\"14cf9c6be92efcfe628aabd32b02c85e763615ddd430861bc18f6d366e4c4fd5-1\\\"},{\\\"value\\\":10000,\\\"outpoint\\\":\\\"9407b31fd0159dc4dd3f5377e3b18e4b4aafef2977a52e76b95c3f899cbb05ad-1\\\"}],\\\"type\\\":0,\\\"dest\\\":\\\"tb1q9m8cc0jkjlc9zwvea5a2365u6px3yu646vgez4\\\",\\\"params\\\":\\\"testnet\\\",\\\"version\\\":2,\\\"fee_amount\\\":314,\\\"fingerprint\\\":\\\"eed8a1cd\\\",\\\"step\\\":4,\\\"collabChange\\\":\\\"tb1qv4ak4l0w76qflk4uulavu22kxtaajnltkzxyq5\\\",\\\"id\\\":\\\"testID\\\",\\\"account\\\":0,\\\"ts\\\":123456}}\",\"done\":true}",
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
