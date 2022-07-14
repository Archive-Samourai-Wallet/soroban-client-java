package com.samourai.soroban.client.cahoots;

import com.samourai.soroban.cahoots.CahootsContext;
import com.samourai.soroban.cahoots.ManualCahootsService;
import com.samourai.soroban.client.OnlineSorobanInteraction;
import com.samourai.soroban.client.SorobanMessage;
import com.samourai.soroban.client.SorobanService;
import com.samourai.soroban.client.meeting.SorobanMeetingService;
import com.samourai.soroban.client.meeting.SorobanRequestMessage;
import com.samourai.soroban.client.meeting.SorobanResponseMessage;
import com.samourai.soroban.client.rpc.RpcService;
import com.samourai.wallet.bip47.BIP47UtilGeneric;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.bipFormat.BipFormatSupplier;
import com.samourai.wallet.cahoots.CahootsType;
import com.samourai.wallet.cahoots.CahootsWallet;
import com.samourai.wallet.cahoots.multi.MultiCahootsService;
import com.samourai.wallet.cahoots.stonewallx2.Stonewallx2Service;
import com.samourai.wallet.cahoots.stowaway.StowawayService;
import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanCahootsService {
  private final Logger log = LoggerFactory.getLogger(SorobanCahootsService.class);

  private OnlineCahootsService onlineCahootsService;
  private SorobanService sorobanService;
  private SorobanMeetingService sorobanMeetingService;
  private ManualCahootsService manualCahootsService;

  public SorobanCahootsService(
      BIP47UtilGeneric bip47Util,
      BipFormatSupplier bipFormatSupplier,
      NetworkParameters params,
      RpcService rpcService) {
    StowawayService stowawayService = new StowawayService(bipFormatSupplier, params);
    Stonewallx2Service stonewallx2Service = new Stonewallx2Service(bipFormatSupplier, params);
    MultiCahootsService multiCahootsService =
        new MultiCahootsService(bipFormatSupplier, params, stonewallx2Service, stowawayService);

    this.onlineCahootsService =
        new OnlineCahootsService(stowawayService, stonewallx2Service, multiCahootsService);
    this.sorobanService = new SorobanService(bip47Util, params, rpcService);
    this.sorobanMeetingService = new SorobanMeetingService(rpcService);
    this.manualCahootsService =
        new ManualCahootsService(stowawayService, stonewallx2Service, multiCahootsService);
  }

  protected void checkTor() throws Exception {
    // override here
  }

  // meeting

  public Observable<SorobanRequestMessage> sendMeetingRequest(
      CahootsWallet cahootsWallet, PaymentCode paymentCodeCounterParty, CahootsType type)
      throws Exception {
    checkTor();
    return sorobanMeetingService.sendMeetingRequest(cahootsWallet, paymentCodeCounterParty, type);
  }

  public Observable<SorobanRequestMessage> receiveMeetingRequest(
      CahootsWallet cahootsWallet, long timeoutMs) throws Exception {
    checkTor();
    return sorobanMeetingService.receiveMeetingRequest(cahootsWallet, timeoutMs);
  }

  public Observable<SorobanResponseMessage> sendMeetingResponse(
      CahootsWallet cahootsWallet,
      PaymentCode paymentCodeCounterParty,
      SorobanRequestMessage request,
      boolean accept)
      throws Exception {
    checkTor();
    return sorobanMeetingService.sendMeetingResponse(
        cahootsWallet, paymentCodeCounterParty, request, accept);
  }

  public Observable<SorobanResponseMessage> receiveMeetingResponse(
      CahootsWallet cahootsWallet,
      PaymentCode paymentCodeCounterparty,
      SorobanRequestMessage request,
      long timeoutMs)
      throws Exception {
    checkTor();
    return sorobanMeetingService.receiveMeetingResponse(
        cahootsWallet, paymentCodeCounterparty, request, timeoutMs);
  }

  // cahoots

  public Observable<SorobanMessage> initiator(
      CahootsContext cahootsContext,
      PaymentCode paymentCodeCounterparty,
      long timeoutMs,
      Consumer<OnlineSorobanInteraction> onInteraction)
      throws Exception {
    checkTor();
    OnlineCahootsMessage message = onlineCahootsService.initiate(cahootsContext);
    return sorobanService.initiator(
        cahootsContext,
        onlineCahootsService,
        paymentCodeCounterparty,
        timeoutMs,
        message,
        onInteraction);
  }

  public Observable<SorobanMessage> contributor(
      CahootsContext cahootsContext, PaymentCode paymentCodeInitiator, long timeoutMs)
      throws Exception {
    checkTor();
    return sorobanService.contributor(
        cahootsContext, onlineCahootsService, paymentCodeInitiator, timeoutMs);
  }

  public SorobanService getSorobanService() {
    return sorobanService;
  }

  public OnlineCahootsService getOnlineCahootsService() {
    return onlineCahootsService;
  }

  public SorobanMeetingService getSorobanMeetingService() {
    return sorobanMeetingService;
  }

  public ManualCahootsService getManualCahootsService() {
    return manualCahootsService;
  }
}
