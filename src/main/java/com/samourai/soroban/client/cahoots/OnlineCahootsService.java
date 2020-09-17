package com.samourai.soroban.client.cahoots;

import com.samourai.wallet.cahoots.*;
import com.samourai.wallet.soroban.client.SorobanMessage;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnlineCahootsService extends ManualCahootsService {
  private static final Logger log = LoggerFactory.getLogger(ManualCahootsService.class);

  public OnlineCahootsService(NetworkParameters params, CahootsWallet cahootsWallet) {
    super(params, cahootsWallet);
  }

  @Override
  public OnlineCahootsMessage parse(String payload) throws Exception {
    return OnlineCahootsMessage.parse(payload);
  }

  @Override
  public OnlineCahootsMessage newStonewallx2(int account, long amount, String address)
      throws Exception {
    ManualCahootsMessage manualCahootsMessage = super.newStonewallx2(account, amount, address);
    return new OnlineCahootsMessage(manualCahootsMessage.getCahoots(), false);
  }

  @Override
  public OnlineCahootsMessage newStowaway(int account, long amount) throws Exception {
    ManualCahootsMessage manualCahootsMessage = super.newStowaway(account, amount);
    return new OnlineCahootsMessage(manualCahootsMessage.getCahoots(), false);
  }

  @Override
  public OnlineCahootsMessage reply(int account, ManualCahootsMessage request) throws Exception {
    ManualCahootsMessage manualCahootsResponse = super.reply(account, request);
    if (CahootsTypeUser.SENDER.equals(manualCahootsResponse.getTypeUser())
        && manualCahootsResponse.isDone()) {
      // interaction: TX_BROADCAST
      // wait for TX to be broadcasted before replying last Cahoots
      return new OnlineCahootsInteraction(
          manualCahootsResponse.getCahoots(), CahootsInteraction.TX_BROADCAST);
    }
    return new OnlineCahootsMessage(manualCahootsResponse.getCahoots(), false);
  }

  public OnlineCahootsMessage confirmTxBroadcast(SorobanMessage message) throws Exception {
    OnlineCahootsMessage cahootsMessage =
        new OnlineCahootsMessage(((OnlineCahootsInteraction) message).getCahoots(), true);
    return cahootsMessage;
  }
}
