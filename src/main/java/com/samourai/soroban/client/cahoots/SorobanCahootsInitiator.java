package com.samourai.soroban.client.cahoots;

import com.samourai.http.client.IHttpClient;
import com.samourai.soroban.client.SorobanService;
import com.samourai.soroban.client.meeting.SorobanMeetingService;
import com.samourai.soroban.client.meeting.SorobanRequestMessage;
import com.samourai.soroban.client.meeting.SorobanResponseMessage;
import com.samourai.wallet.bip47.BIP47UtilGeneric;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.cahoots.CahootsMessage;
import com.samourai.wallet.cahoots.CahootsService;
import com.samourai.wallet.cahoots.CahootsType;
import com.samourai.wallet.cahoots.CahootsWallet;
import com.samourai.wallet.soroban.client.SorobanMessage;
import io.reactivex.Observable;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanCahootsInitiator extends AbstractSorobanCahoots {
  private final Logger log = LoggerFactory.getLogger(SorobanCahootsInitiator.class);

  public SorobanCahootsInitiator(
      CahootsService cahootsService,
      SorobanService sorobanService,
      SorobanMeetingService sorobanMeetingService) {
    super(cahootsService, sorobanService, sorobanMeetingService);
  }

  public SorobanCahootsInitiator(
      BIP47UtilGeneric bip47Util,
      NetworkParameters params,
      CahootsWallet cahootsWallet,
      BIP47Wallet bip47Wallet,
      IHttpClient httpClient) {
    super(bip47Util, params, cahootsWallet, bip47Wallet, httpClient);
  }

  // meeting

  public Observable<SorobanRequestMessage> sendMeetingRequest(
      PaymentCode paymentCodeCounterParty, String description, CahootsType type) throws Exception {
    checkTor();
    return sorobanMeetingService.sendMeetingRequest(paymentCodeCounterParty, description, type);
  }

  public Observable<SorobanResponseMessage> receiveMeetingResponse(
      SorobanRequestMessage request, long timeoutMs) throws Exception {
    checkTor();
    return sorobanMeetingService.receiveMeetingResponse(request, timeoutMs);
  }

  // cahoots

  public Observable<SorobanMessage> newStonewallx2(
      int account, long amount, String address, PaymentCode paymentCodeCounterparty, long timeoutMs)
      throws Exception {
    checkTor();
    CahootsMessage message = cahootsService.newStonewallx2(account, amount, address);
    return sorobanService.initiator(
        account, cahootsService, paymentCodeCounterparty, timeoutMs, message);
  }

  public Observable<SorobanMessage> newStowaway(
      int account, long amount, PaymentCode paymentCodeCounterparty, long timeoutMs)
      throws Exception {
    checkTor();
    CahootsMessage message = cahootsService.newStowaway(account, amount);
    return sorobanService.initiator(
        account, cahootsService, paymentCodeCounterparty, timeoutMs, message);
  }
}
