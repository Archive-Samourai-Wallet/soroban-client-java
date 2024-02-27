package com.samourai.soroban.client;

import com.samourai.soroban.client.cahoots.OnlineCahootsMessage;
import com.samourai.wallet.cahoots.manual.ManualCahootsMessage;
import com.samourai.wallet.sorobanClient.SorobanInteraction;

public class OnlineSorobanInteraction extends SorobanInteraction {
  private SorobanInteraction interaction;
  private SorobanInteractionHandler interactionHandler;

  public OnlineSorobanInteraction(
      SorobanInteraction interaction, SorobanInteractionHandler interactionHandler) {
    super(interaction);
    this.interaction = interaction;
    this.interactionHandler = interactionHandler;
  }

  public SorobanInteraction getInteraction() {
    return interaction;
  }

  public void sorobanAccept() throws Exception {
    ManualCahootsMessage reply = (ManualCahootsMessage) interaction.getReplyAccept();
    OnlineCahootsMessage onlineReply = new OnlineCahootsMessage(reply);
    interactionHandler.replyInteractive(onlineReply);
  }

  public void sorobanReject(String reason) throws Exception {
    interactionHandler.replyInteractive(new Exception(reason));
  }
}
