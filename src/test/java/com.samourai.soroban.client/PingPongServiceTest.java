package com.samourai.soroban.client;

import com.samourai.http.client.IHttpClient;
import com.samourai.http.client.JavaHttpClient;
import com.samourai.soroban.client.pingPong.PingPongMessage;
import com.samourai.soroban.client.pingPong.PingPongService;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.soroban.client.SorobanMessage;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
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
    doTest(1, "{\"value\":\"PING\",\"iteration\":1,\"lastMessage\":true}");
    Thread.sleep(3000); // wait Soroban server cleanup

    doTest(2, "{\"value\":\"PONG\",\"iteration\":2,\"lastMessage\":true}");
    Thread.sleep(3000);

    doTest(3, "{\"value\":\"PING\",\"iteration\":3,\"lastMessage\":true}");
    Thread.sleep(3000);

    doTest(4, "{\"value\":\"PONG\",\"iteration\":4,\"lastMessage\":true}");
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
                IHttpClient httpClient = new JavaHttpClient();
                SorobanService sorobanService =
                    new SorobanService(params, bip47walletInitiator, pingPongService, httpClient);
                try {
                  // run soroban as initiator
                  boolean last = ITERATIONS == 1;
                  PingPongMessage message = new PingPongMessage(PingPongMessage.VALUES.PING, last);
                  Subject<SorobanMessage> onMessage = BehaviorSubject.create();
                  SorobanMessage lastMessage =
                      sorobanService.initiator(paymentCodeCounterparty, message, onMessage);
                  Assertions.assertEquals(lastPayload, lastMessage.toPayload());
                } catch (Exception e) {
                  Assertions.fail(e);
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
                PingPongService pingPongService = new PingPongService(ITERATIONS);
                IHttpClient httpClient = new JavaHttpClient();
                SorobanService sorobanService =
                    new SorobanService(
                        params, bip47walletCounterparty, pingPongService, httpClient);
                try {
                  // run soroban as contributor
                  Subject<SorobanMessage> onMessage = BehaviorSubject.create();
                  SorobanMessage lastMessage =
                      sorobanService.contributor(
                          paymentCodeInitiator, SOROBAN_TIMEOUT_MS, onMessage);
                  Assertions.assertEquals(lastPayload, lastMessage.toPayload());
                } catch (Exception e) {
                  Assertions.fail(e);
                } finally {
                  try {
                    sorobanService.close();
                  } catch (Exception e) {
                  }
                }
              }
            });
    threadContributor.start();

    synchronized (threadInitiator) {
      threadInitiator.wait();
    }
    log.info("threadInitiator ended");
  }
}
