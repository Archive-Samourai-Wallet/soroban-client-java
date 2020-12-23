package com.samourai.soroban.client;

import com.samourai.http.client.IHttpClient;
import com.samourai.http.client.JavaHttpClient;
import com.samourai.soroban.client.meeting.SorobanMeetingService;
import com.samourai.soroban.client.meeting.SorobanRequestMessage;
import com.samourai.soroban.client.meeting.SorobanResponseMessage;
import com.samourai.soroban.client.rpc.RpcClient;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.cahoots.CahootsType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanMeetingServiceTest extends AbstractTest {
  private static final Logger log = LoggerFactory.getLogger(SorobanMeetingServiceTest.class);

  private static final String SEED_WORDS = "all all all all all all all all all all all all";
  private static final String SEED_PASSPHRASE_INITIATOR = "initiator";
  private static final String SEED_PASSPHRASE_COUNTERPARTY = "counterparty";

  @Test
  public void meet() throws Exception {
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
                IHttpClient httpClient = new JavaHttpClient(TIMEOUT_MS);
                RpcClient rpcClient = new RpcClient(httpClient, params);
                final SorobanMeetingService sorobanMeetingService =
                    new SorobanMeetingService(
                        bip47Util, params, PROVIDER_JAVA, bip47walletInitiator, rpcClient);

                try {
                  // request soroban meeeting
                  SorobanRequestMessage request =
                      sorobanMeetingService
                          .sendMeetingRequest(paymentCodeCounterparty, CahootsType.STONEWALLX2)
                          .blockingSingle();
                  SorobanResponseMessage response =
                      sorobanMeetingService
                          .receiveMeetingResponse(paymentCodeCounterparty, request, TIMEOUT_MS)
                          .blockingSingle();
                  Assertions.assertTrue(response.isAccept());
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
                RpcClient rpcClient = new RpcClient(httpClient, params);
                SorobanMeetingService sorobanMeetingService =
                    new SorobanMeetingService(
                        bip47Util, params, PROVIDER_JAVA, bip47walletCounterparty, rpcClient);

                try {
                  // listen for Soroban requests
                  SorobanRequestMessage requestMessage =
                      sorobanMeetingService.receiveMeetingRequest(TIMEOUT_MS).blockingSingle();
                  Assertions.assertEquals(CahootsType.STONEWALLX2, requestMessage.getType());
                  Assertions.assertEquals(
                      paymentCodeInitiator.toString(), requestMessage.getSender());

                  // response accept
                  sorobanMeetingService
                      .sendMeetingResponse(paymentCodeInitiator, requestMessage, true)
                      .subscribe();
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
    log.info("*** SOROBAN MEETING SUCCESS ***");
  }
}
