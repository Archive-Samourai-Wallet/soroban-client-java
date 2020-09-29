package com.samourai.soroban.client.cahoots;

import com.samourai.wallet.cahoots.CahootsWallet;
import com.samourai.wallet.soroban.cahoots.CahootsContext;
import com.samourai.wallet.soroban.cahoots.ManualCahootsMessage;
import com.samourai.wallet.soroban.cahoots.ManualCahootsService;
import com.samourai.wallet.soroban.client.SorobanInteraction;
import com.samourai.wallet.soroban.client.SorobanMessage;
import com.samourai.wallet.soroban.client.SorobanReply;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnlineCahootsService extends ManualCahootsService {
  private static final Logger log = LoggerFactory.getLogger(ManualCahootsService.class);

  public OnlineCahootsService(CahootsWallet cahootsWallet) {
    super(cahootsWallet);
  }

  @Override
  public OnlineCahootsMessage parse(String payload) throws Exception {
    return OnlineCahootsMessage.parse(payload);
  }

  @Override
  public OnlineCahootsMessage initiate(int account, CahootsContext cahootsContext)
      throws Exception {
    ManualCahootsMessage manualCahootsMessage = super.initiate(account, cahootsContext);
    return new OnlineCahootsMessage(manualCahootsMessage);
  }

  @Override
  public SorobanReply reply(
      final int account, final CahootsContext cahootsContext, final ManualCahootsMessage request)
      throws Exception {
    SorobanReply reply = super.reply(account, cahootsContext, request);
    SorobanReply onlineReply;
    if (reply instanceof ManualCahootsMessage) {
      // SorobanMessage
      onlineReply = new OnlineCahootsMessage((ManualCahootsMessage) reply);
    } else if (reply instanceof SorobanInteraction) {
      // SorobanInteraction
      final SorobanInteraction interaction = (SorobanInteraction) reply;
      Callable<SorobanMessage> onAccept =
          new Callable<SorobanMessage>() {
            @Override
            public SorobanMessage call() throws Exception {
              ManualCahootsMessage response = (ManualCahootsMessage) interaction.accept();
              return new OnlineCahootsMessage(response);
            }
          };
      onlineReply =
          new SorobanInteraction(
              interaction.getRequest(), interaction.getTypeInteraction(), onAccept);
    } else {
      throw new Exception("Unknown message type: " + reply.getClass());
    }
    return onlineReply;
  }
}
