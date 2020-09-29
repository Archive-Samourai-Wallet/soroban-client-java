package com.samourai.soroban.client;

import com.samourai.http.client.IHttpClient;
import com.samourai.http.client.JavaHttpClient;
import com.samourai.soroban.client.pingPong.PingPongMessage;
import com.samourai.soroban.client.pingPong.PingPongService;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.soroban.client.SorobanMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PingPongServiceTest extends AbstractTest {
  private static final Logger log = LoggerFactory.getLogger(PingPongServiceTest.class);

  private static final String SEED_WORDS = "all all all all all all all all all all all all";
  private static final String SEED_PASSPHRASE_INITIATOR = "initiator";
  private static final String SEED_PASSPHRASE_COUNTERPARTY = "counterparty";

  @Test
  public void pingPong() throws Exception {
    doTest(1, "{\"value\":\"PING\",\"iteration\":1,\"done\":true}");
    Thread.sleep(3000); // wait Soroban server cleanup

    doTest(2, "{\"value\":\"PONG\",\"iteration\":2,\"done\":true}");
    Thread.sleep(3000);

    doTest(3, "{\"value\":\"PING\",\"iteration\":3,\"done\":true}");
    Thread.sleep(3000);

    doTest(4, "{\"value\":\"PONG\",\"iteration\":4,\"done\":true}");
  }

  private void doTest(final int ITERATIONS, final String lastPayload) throws Exception {
    log.info("### doTest " + ITERATIONS);
    final BIP47Wallet bip47walletInitiator = bip47Wallet(SEED_WORDS, SEED_PASSPHRASE_INITIATOR);
    final BIP47Wallet bip47walletCounterparty =
        bip47Wallet(SEED_WORDS, SEED_PASSPHRASE_COUNTERPARTY);

    final PaymentCode paymentCodeInitiator = bip47Util.getPaymentCode(bip47walletInitiator);
    final PaymentCode paymentCodeCounterparty = bip47Util.getPaymentCode(bip47walletCounterparty);

    // run initiator
    Thread threadInitiator =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                // instanciate services
                PingPongService pingPongService = new PingPongService(ITERATIONS);
                IHttpClient httpClient = new JavaHttpClient(TIMEOUT_MS);
                SorobanService sorobanService =
                    new SorobanService(
                        bip47Util, params, PROVIDER_JAVA, bip47walletInitiator, httpClient);
                try {
                  // run soroban as initiator
                  boolean last = ITERATIONS == 1;
                  PingPongMessage message = new PingPongMessage(PingPongMessage.VALUES.PING, last);
                  SorobanMessage lastMessage =
                      sorobanService
                          .initiator(
                              0,
                              null,
                              pingPongService,
                              paymentCodeCounterparty,
                              TIMEOUT_MS,
                              message)
                          .blockingLast();
                  Assertions.assertEquals(lastPayload, lastMessage.toPayload());
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
                PingPongService pingPongService = new PingPongService(ITERATIONS);
                IHttpClient httpClient = new JavaHttpClient(TIMEOUT_MS);
                SorobanService sorobanService =
                    new SorobanService(
                        bip47Util, params, PROVIDER_JAVA, bip47walletCounterparty, httpClient);
                try {
                  // run soroban as contributor
                  SorobanMessage lastMessage =
                      sorobanService
                          .contributor(0, null, pingPongService, paymentCodeInitiator, TIMEOUT_MS)
                          .blockingLast();
                  Assertions.assertEquals(lastPayload, lastMessage.toPayload());
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
    log.info("threadInitiator ended");
  }
}
