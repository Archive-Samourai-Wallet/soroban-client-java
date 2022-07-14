package com.samourai.soroban.client;

import com.samourai.soroban.client.meeting.SorobanRequestMessage;
import com.samourai.soroban.client.meeting.SorobanResponseMessage;
import com.samourai.wallet.cahoots.CahootsType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanMeetingServiceTest extends AbstractTest {
  private static final Logger log = LoggerFactory.getLogger(SorobanMeetingServiceTest.class);

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  @Test
  public void meet() throws Exception {
    // run initiator
    Thread threadInitiator =
        new Thread(
            () -> {
              try {
                // request soroban meeeting
                SorobanRequestMessage request =
                    sorobanMeetingService
                        .sendMeetingRequest(
                            cahootsWalletInitiator,
                            paymentCodeCounterparty,
                            CahootsType.STONEWALLX2)
                        .blockingSingle();
                SorobanResponseMessage response =
                    sorobanMeetingService
                        .receiveMeetingResponse(
                            cahootsWalletInitiator, paymentCodeCounterparty, request, TIMEOUT_MS)
                        .blockingSingle();
                Assertions.assertTrue(response.isAccept());
              } catch (Exception e) {
                setException(e);
              }
            });
    threadInitiator.start();

    // run contributor
    Thread threadContributor =
        new Thread(
            () -> {
              try {
                // listen for Soroban requests
                SorobanRequestMessage requestMessage =
                    sorobanMeetingService
                        .receiveMeetingRequest(cahootsWalletCounterparty, TIMEOUT_MS)
                        .blockingSingle();
                Assertions.assertEquals(CahootsType.STONEWALLX2, requestMessage.getType());
                Assertions.assertEquals(
                    paymentCodeInitiator.toString(), requestMessage.getSender());

                // response accept
                sorobanMeetingService
                    .sendMeetingResponse(
                        cahootsWalletCounterparty, paymentCodeInitiator, requestMessage, true)
                    .subscribe();
              } catch (Exception e) {
                setException(e);
              }
            });
    threadContributor.start();

    assertNoException();
    threadInitiator.join();
    threadContributor.join();

    assertNoException();
    log.info("*** SOROBAN MEETING SUCCESS ***");
  }
}
