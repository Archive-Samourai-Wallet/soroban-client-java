package com.samourai.soroban.client.wallet.counterparty;

import com.samourai.soroban.client.SorobanService;
import com.samourai.soroban.client.cahoots.OnlineCahootsMessage;
import com.samourai.soroban.client.cahoots.OnlineCahootsService;
import com.samourai.soroban.client.meeting.SorobanMeetingService;
import com.samourai.soroban.client.meeting.SorobanRequestMessage;
import com.samourai.soroban.client.meeting.SorobanResponseMessage;
import com.samourai.soroban.client.rpc.NoValueRpcException;
import com.samourai.soroban.client.wallet.SorobanWallet;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.cahoots.Cahoots;
import com.samourai.wallet.cahoots.CahootsContext;
import com.samourai.wallet.cahoots.CahootsWallet;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanWalletCounterparty extends SorobanWallet {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private boolean listening;

  public SorobanWalletCounterparty(
      OnlineCahootsService onlineCahootsService,
      SorobanService sorobanService,
      SorobanMeetingService sorobanMeetingService,
      CahootsWallet cahootsWallet) {
    super(onlineCahootsService, sorobanService, sorobanMeetingService, cahootsWallet);
  }

  public void startListening(SorobanCounterpartyListener listener) throws Exception {
    if (listening) {
      throw new Exception("Already listening");
    }
    this.listening = true;
    Thread t =
        new Thread(
            () -> {
              while (listening) {
                log.info("Listening for Soroban requests...");
                try {
                  SorobanRequestMessage request = receiveMeetingRequest();
                  log.info("New Soroban request: " + request);
                  listener.onRequest(request);
                } catch (NoValueRpcException e) {
                  // ignore
                } catch (Exception e) {
                  log.error("Failed listening for Soroban requests", e);
                }
              }
              log.info("Stopped listening for Soroban requests...");
            },
            "sorobanWalletCounterparty-listen");
    t.setDaemon(true);
    t.start();
  }

  public void stopListening() {
    listening = false;
  }

  public boolean isListening() {
    return listening;
  }

  public SorobanRequestMessage receiveMeetingRequest() throws Exception {
    return sorobanMeetingService.receiveMeetingRequest(rpcSession, getTimeoutMeetingMs());
  }

  public Single<SorobanResponseMessage> sendMeetingResponse(
      SorobanRequestMessage cahootsRequest, boolean accept) {
    return sorobanMeetingService.sendMeetingResponse(rpcSession, cahootsRequest, accept);
  }

  public Single<SorobanResponseMessage> decline(SorobanRequestMessage cahootsRequest)
      throws Exception {
    // decline request
    return sendMeetingResponse(cahootsRequest, false)
        .doOnSuccess(
            response -> {
              log.info("Soroban request declined: " + cahootsRequest);
            });
  }

  public Single<SorobanResponseMessage> accept(SorobanRequestMessage cahootsRequest)
      throws Exception {
    // decline request
    return sendMeetingResponse(cahootsRequest, true)
        .doOnSuccess(
            response -> {
              log.info("Soroban request accepted: " + cahootsRequest);
            });
  }

  public Cahoots acceptAndCounterparty(
      SorobanRequestMessage cahootsRequest, SorobanCounterpartyListener listener) throws Exception {
    // accept request
    asyncUtil.blockingGet(sendMeetingResponse(cahootsRequest, true));
    log.info("Soroban request accepted => starting Cahoots... " + cahootsRequest);

    // start Cahoots
    CahootsContext cahootsContext = listener.newCounterpartyContext(cahootsWallet, cahootsRequest);
    PaymentCode paymentCodeSender = cahootsRequest.getSender();
    Consumer<OnlineCahootsMessage> onProgress = sorobanMessage -> listener.progress(sorobanMessage);
    return counterparty(cahootsContext, paymentCodeSender, onProgress);
  }

  public Cahoots counterparty(
      CahootsContext cahootsContext,
      PaymentCode paymentCodeSender,
      Consumer<OnlineCahootsMessage> onProgress)
      throws Exception {
    return asyncUtil.blockingGet(
        sorobanService
            .counterparty(
                cahootsContext,
                rpcSession,
                onlineCahootsService,
                paymentCodeSender,
                timeoutDialogMs)
            // notify on progress
            .map(sorobanMessage -> (OnlineCahootsMessage) sorobanMessage)
            .doOnNext(onProgress)
            // return Cahoots on success
            .lastOrError()
            .map(onlineCahootsMessage -> onlineCahootsMessage.getCahoots()));
  }
}
