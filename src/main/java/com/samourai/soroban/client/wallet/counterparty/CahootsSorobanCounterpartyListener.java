package com.samourai.soroban.client.wallet.counterparty;

import com.samourai.soroban.client.cahoots.OnlineCahootsMessage;
import com.samourai.soroban.client.meeting.SorobanRequestMessage;
import com.samourai.wallet.cahoots.CahootsContext;
import com.samourai.wallet.cahoots.CahootsWallet;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CahootsSorobanCounterpartyListener implements SorobanCounterpartyListener {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  protected int account;

  public CahootsSorobanCounterpartyListener(int account) {
    this.account = account;
  }

  @Override
  public CahootsContext newCounterpartyContext(
      CahootsWallet cahootsWallet, SorobanRequestMessage cahootsRequest) throws Exception {
    return CahootsContext.newCounterparty(cahootsWallet, cahootsRequest.getType(), account);
  }

  @Override
  public void progress(OnlineCahootsMessage message) throws Exception {
    log.info("[counterparty] progress: " + message);
  }

  @Override
  public void onRequest(SorobanRequestMessage sorobanRequest) throws Exception {
    log.info("[counterparty] new Cahoots request received: " + sorobanRequest);
    // override here to accept or decline requests
  }
}
