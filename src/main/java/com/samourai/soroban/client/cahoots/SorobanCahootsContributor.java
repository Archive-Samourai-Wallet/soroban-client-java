package com.samourai.soroban.client.cahoots;

import com.samourai.http.client.IHttpClient;
import com.samourai.soroban.client.SorobanService;
import com.samourai.soroban.client.meeting.SorobanMeetingService;
import com.samourai.soroban.client.meeting.SorobanRequestMessage;
import com.samourai.soroban.client.meeting.SorobanResponseMessage;
import com.samourai.wallet.bip47.BIP47UtilGeneric;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.cahoots.CahootsService;
import com.samourai.wallet.cahoots.CahootsWallet;
import com.samourai.wallet.soroban.client.SorobanMessage;
import io.reactivex.Observable;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanCahootsContributor extends AbstractSorobanCahoots {
  private final Logger log = LoggerFactory.getLogger(SorobanCahootsContributor.class);

  public SorobanCahootsContributor(
      CahootsService cahootsService,
      SorobanService sorobanService,
      SorobanMeetingService sorobanMeetingService) {
    super(cahootsService, sorobanService, sorobanMeetingService);
  }

  public SorobanCahootsContributor(
      BIP47UtilGeneric bip47Util,
      NetworkParameters params,
      String provider,
      CahootsWallet cahootsWallet,
      BIP47Wallet bip47Wallet,
      IHttpClient httpClient) {
    super(bip47Util, params, provider, cahootsWallet, bip47Wallet, httpClient);
  }

  // meeting

  public Observable<SorobanRequestMessage> receiveMeetingRequest(long timeoutMs) throws Exception {
    checkTor();
    return sorobanMeetingService.receiveMeetingRequest(timeoutMs);
  }

  public Observable<SorobanResponseMessage> sendMeetingResponse(
      PaymentCode paymentCodeCounterParty, SorobanRequestMessage request, boolean accept)
      throws Exception {
    checkTor();
    return sorobanMeetingService.sendMeetingResponse(paymentCodeCounterParty, request, accept);
  }

  // cahoots

  public Observable<SorobanMessage> contributor(
      int account, PaymentCode paymentCodeInitiator, long timeoutMs) throws Exception {
    checkTor();
    return sorobanService.contributor(account, cahootsService, paymentCodeInitiator, timeoutMs);
  }
}
