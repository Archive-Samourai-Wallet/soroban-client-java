package com.samourai.soroban.client.meeting;

import com.samourai.soroban.client.AbstractTest;
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
                    asyncUtil.blockingGet(
                        sorobanMeetingService.sendMeetingRequest(
                            cahootsWalletInitiator,
                            paymentCodeCounterparty,
                            CahootsType.STONEWALLX2));
                SorobanResponseMessage response =
                    asyncUtil.blockingGet(
                        sorobanMeetingService.receiveMeetingResponse(
                            cahootsWalletInitiator, paymentCodeCounterparty, request, TIMEOUT_MS));
                Assertions.assertTrue(response.isAccept());
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
                // listen for Soroban requests
                SorobanRequestMessage requestMessage =
                    asyncUtil.blockingGet(
                        sorobanMeetingService.receiveMeetingRequest(
                            cahootsWalletCounterparty, TIMEOUT_MS));
                Assertions.assertEquals(CahootsType.STONEWALLX2, requestMessage.getType());
                Assertions.assertEquals(
                    paymentCodeInitiator.toString(), requestMessage.getSender());

                // response accept
                sorobanMeetingService
                    .sendMeetingResponse(cahootsWalletCounterparty, requestMessage, true)
                    .subscribe();
              } catch (Exception e) {
                setException(e);
              }
            });
    threadCounterparty.start();

    assertNoException();
    threadInitiator.join();
    threadCounterparty.join();

    assertNoException();
    log.info("*** SOROBAN MEETING SUCCESS ***");
  }
}