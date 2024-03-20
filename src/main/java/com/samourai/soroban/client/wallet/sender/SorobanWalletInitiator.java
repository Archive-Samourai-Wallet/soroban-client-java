package com.samourai.soroban.client.wallet.sender;

import com.samourai.soroban.client.OnlineSorobanInteraction;
import com.samourai.soroban.client.SorobanService;
import com.samourai.soroban.client.cahoots.OnlineCahootsMessage;
import com.samourai.soroban.client.cahoots.OnlineCahootsService;
import com.samourai.soroban.client.meeting.SorobanMeetingService;
import com.samourai.soroban.client.meeting.SorobanResponseMessage;
import com.samourai.soroban.client.wallet.SorobanWallet;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.cahoots.Cahoots;
import com.samourai.wallet.cahoots.CahootsContext;
import com.samourai.wallet.cahoots.CahootsType;
import com.samourai.wallet.cahoots.CahootsWallet;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanWalletInitiator extends SorobanWallet {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public SorobanWalletInitiator(
      OnlineCahootsService onlineCahootsService,
      SorobanService sorobanService,
      SorobanMeetingService sorobanMeetingService,
      CahootsWallet cahootsWallet) {
    super(onlineCahootsService, sorobanService, sorobanMeetingService, cahootsWallet);
  }

  public Single<SorobanResponseMessage> meet(
      CahootsType cahootsType, PaymentCode paymentCodeCounterparty) {
    // send request
    return sorobanMeetingService
        .sendMeetingRequest(rpcSession, paymentCodeCounterparty, cahootsType)
        // receive response
        .map(
            request ->
                sorobanMeetingService.receiveMeetingResponse(
                    rpcSession, paymentCodeCounterparty, request, timeoutMeetingMs));
  }

  public Cahoots meetAndInitiate(CahootsContext cahootsContext, PaymentCode paymentCodeCounterparty)
      throws Exception {
    SorobanInitiatorListener listener = new CahootsSorobanInitiatorListener();
    return meetAndInitiate(cahootsContext, paymentCodeCounterparty, listener);
  }

  public Cahoots meetAndInitiate(
      CahootsContext cahootsContext,
      PaymentCode paymentCodeCounterparty,
      SorobanInitiatorListener listener)
      throws Exception {
    // meet
    SorobanResponseMessage meetingResponse =
        asyncUtil.blockingGet(meet(cahootsContext.getCahootsType(), paymentCodeCounterparty));

    listener.onResponse(meetingResponse);
    if (!meetingResponse.isAccept()) {
      throw new Exception("Partner declined the Cahoots request");
    }
    log.info("Soroban request accepted => starting Cahoots... " + meetingResponse);

    // start Cahoots
    Consumer<OnlineCahootsMessage> onProgress = sorobanMessage -> listener.progress(sorobanMessage);
    Consumer<OnlineSorobanInteraction> onInteraction =
        sorobanMessage -> listener.onInteraction(sorobanMessage);
    return initiator(cahootsContext, paymentCodeCounterparty, onProgress, onInteraction);
  }

  public Cahoots initiator(
      CahootsContext cahootsContext,
      PaymentCode paymentCodeCounterparty,
      Consumer<OnlineCahootsMessage> onProgress,
      Consumer<OnlineSorobanInteraction> onInteraction)
      throws Exception {
    OnlineCahootsMessage message = onlineCahootsService.initiate(cahootsContext);
    return asyncUtil.blockingGet(
        sorobanService
            .initiator(
                cahootsContext,
                rpcSession,
                onlineCahootsService,
                paymentCodeCounterparty,
                timeoutDialogMs,
                message,
                interaction -> onInteraction.accept(interaction))
            // notify on progress
            .map(sorobanMessage -> (OnlineCahootsMessage) sorobanMessage)
            .doOnNext(sorobanMessage -> onProgress.accept(sorobanMessage))
            // return Cahoots on success
            .lastOrError()
            .map(onlineCahootsMessage -> onlineCahootsMessage.getCahoots()));
  }
}
