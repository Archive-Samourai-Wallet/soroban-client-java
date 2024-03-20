package com.samourai.soroban.client.wallet.counterparty;

import com.samourai.soroban.client.meeting.SorobanRequestMessage;
import com.samourai.wallet.cahoots.CahootsContext;
import com.samourai.wallet.cahoots.CahootsType;
import com.samourai.wallet.cahoots.CahootsWallet;
import com.samourai.wallet.cahoots.multi.MultiCahootsContext;
import com.samourai.wallet.xmanagerClient.XManagerClient;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiCahootsSorobanCounterpartyListener extends CahootsSorobanCounterpartyListener {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private XManagerClient xManagerClient;

  public MultiCahootsSorobanCounterpartyListener(int account, XManagerClient xManagerClient) {
    super(account);
    this.xManagerClient = xManagerClient;
  }

  @Override
  public CahootsContext newCounterpartyContext(
      CahootsWallet cahootsWallet, SorobanRequestMessage cahootsRequest) throws Exception {
    if (cahootsRequest.getType() == CahootsType.MULTI) {
      // special context for MULTI
      return MultiCahootsContext.newCounterparty(cahootsWallet, account, xManagerClient);
    }
    // standard counterparty context
    return super.newCounterpartyContext(cahootsWallet, cahootsRequest);
  }
}
