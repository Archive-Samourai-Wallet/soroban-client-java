package com.samourai.soroban.client.cahoots;

import com.samourai.http.client.IHttpClient;
import com.samourai.soroban.client.SorobanService;
import com.samourai.soroban.client.meeting.SorobanMeetingService;
import com.samourai.wallet.bip47.BIP47UtilGeneric;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.cahoots.CahootsService;
import com.samourai.wallet.cahoots.CahootsWallet;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSorobanCahoots {
  private final Logger log = LoggerFactory.getLogger(AbstractSorobanCahoots.class);

  protected CahootsService cahootsService;
  protected SorobanService sorobanService;
  protected SorobanMeetingService sorobanMeetingService;

  public AbstractSorobanCahoots(
      CahootsService cahootsService,
      SorobanService sorobanService,
      SorobanMeetingService sorobanMeetingService) {
    this.cahootsService = cahootsService;
    this.sorobanService = sorobanService;
    this.sorobanMeetingService = sorobanMeetingService;
  }

  public AbstractSorobanCahoots(
      BIP47UtilGeneric bip47Util,
      NetworkParameters params,
      CahootsWallet cahootsWallet,
      BIP47Wallet bip47Wallet,
      IHttpClient httpClient) {
    this(
        new CahootsService(params, cahootsWallet),
        new SorobanService(bip47Util, params, bip47Wallet, httpClient),
        new SorobanMeetingService(bip47Util, params, bip47Wallet, httpClient));
  }

  public SorobanService getSorobanService() {
    return sorobanService;
  }

  public CahootsService getCahootsService() {
    return cahootsService;
  }

  protected void checkTor() throws Exception {
    // override here
  }
}
