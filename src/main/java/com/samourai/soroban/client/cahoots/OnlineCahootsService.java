package com.samourai.soroban.client.cahoots;

import com.samourai.soroban.cahoots.CahootsContext;
import com.samourai.soroban.cahoots.ManualCahootsMessage;
import com.samourai.soroban.cahoots.ManualCahootsService;
import com.samourai.soroban.client.SorobanInteraction;
import com.samourai.soroban.client.SorobanReply;
import com.samourai.wallet.cahoots.multi.MultiCahootsService;
import com.samourai.wallet.cahoots.stonewallx2.Stonewallx2Service;
import com.samourai.wallet.cahoots.stowaway.StowawayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnlineCahootsService extends ManualCahootsService {
  private static final Logger log = LoggerFactory.getLogger(ManualCahootsService.class);

  public OnlineCahootsService(
      StowawayService stowawayService,
      Stonewallx2Service stonewallx2Service,
      MultiCahootsService multiCahootsService) {
    super(stowawayService, stonewallx2Service, multiCahootsService);
  }

  @Override
  public OnlineCahootsMessage parse(String payload) throws Exception {
    return OnlineCahootsMessage.parse(payload);
  }

  @Override
  public OnlineCahootsMessage initiate(CahootsContext cahootsContext) throws Exception {
    ManualCahootsMessage manualCahootsMessage = super.initiate(cahootsContext);
    return new OnlineCahootsMessage(manualCahootsMessage);
  }

  @Override
  public SorobanReply reply(final CahootsContext cahootsContext, final ManualCahootsMessage request)
      throws Exception {
    SorobanReply reply = super.reply(cahootsContext, request);
    SorobanReply onlineReply;
    if (reply instanceof ManualCahootsMessage) {
      // wrap SorobanMessage as OnlineCahootsMessage
      onlineReply = new OnlineCahootsMessage((ManualCahootsMessage) reply);
    } else if (reply instanceof SorobanInteraction) {
      // forward SorobanInteraction
      onlineReply = reply;
    } else {
      throw new Exception("Unknown message type: " + reply.getClass());
    }
    return onlineReply;
  }
}
