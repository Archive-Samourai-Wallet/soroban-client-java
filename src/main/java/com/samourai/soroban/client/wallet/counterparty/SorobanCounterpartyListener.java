package com.samourai.soroban.client.wallet.counterparty;

import com.samourai.soroban.client.cahoots.OnlineCahootsMessage;
import com.samourai.soroban.client.meeting.SorobanRequestMessage;
import com.samourai.wallet.cahoots.CahootsContext;
import com.samourai.wallet.cahoots.CahootsWallet;

public interface SorobanCounterpartyListener {
  CahootsContext newCounterpartyContext(
      CahootsWallet cahootsWallet, SorobanRequestMessage cahootsRequest) throws Exception;

  void onRequest(SorobanRequestMessage sorobanRequest) throws Exception;

  void progress(OnlineCahootsMessage message) throws Exception;
}
