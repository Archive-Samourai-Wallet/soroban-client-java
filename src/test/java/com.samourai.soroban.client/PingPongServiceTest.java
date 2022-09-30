package com.samourai.soroban.client;

import com.samourai.soroban.cahoots.CahootsContext;
import com.samourai.soroban.cahoots.Stonewallx2Context;
import com.samourai.soroban.client.pingPong.PingPongMessage;
import com.samourai.soroban.client.pingPong.PingPongService;
import com.samourai.wallet.cahoots.CahootsType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Disabled
public class PingPongServiceTest extends AbstractTest {
  private static final Logger log = LoggerFactory.getLogger(PingPongServiceTest.class);

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

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

    // run initiator
    Thread threadInitiator =
        new Thread(
            () -> {
              // instanciate services
              PingPongService pingPongService = new PingPongService(ITERATIONS);
              try {
                // run soroban as initiator
                boolean last = ITERATIONS == 1;
                PingPongMessage message = new PingPongMessage(PingPongMessage.VALUES.PING, last);
                CahootsContext cahootsContext =
                    Stonewallx2Context.newInitiator(
                        cahootsWalletCounterparty, 0, 0, 1234, "foo", null);
                SorobanMessage lastMessage =
                    sorobanService
                        .initiator(
                            cahootsContext,
                            pingPongService,
                            paymentCodeCounterparty,
                            TIMEOUT_MS,
                            message,
                            onlineSorobanInteraction -> {})
                        .blockingLast();
                Assertions.assertEquals(lastPayload, lastMessage.toPayload());
              } catch (Exception e) {
                setException(e);
              }
            });
    threadInitiator.start();

    // run contributor
    Thread threadContributor =
        new Thread(
            () -> {
              // instanciate services
              PingPongService pingPongService = new PingPongService(ITERATIONS);
              try {
                CahootsContext cahootsContext =
                    CahootsContext.newCounterparty(
                        cahootsWalletCounterparty, CahootsType.STONEWALLX2, 0);
                // run soroban as contributor
                SorobanMessage lastMessage =
                    sorobanService
                        .contributor(
                            cahootsContext, pingPongService, paymentCodeInitiator, TIMEOUT_MS)
                        .blockingLast();
                Assertions.assertEquals(lastPayload, lastMessage.toPayload());
              } catch (Exception e) {
                setException(e);
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
