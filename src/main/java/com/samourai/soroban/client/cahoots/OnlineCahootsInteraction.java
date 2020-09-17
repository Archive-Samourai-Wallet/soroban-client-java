package com.samourai.soroban.client.cahoots;

import com.samourai.wallet.cahoots.Cahoots;

public class OnlineCahootsInteraction extends OnlineCahootsMessage {

  public CahootsInteraction interaction;

  public OnlineCahootsInteraction(Cahoots cahoots, CahootsInteraction interaction) {
    super(cahoots, false);
    this.interaction = interaction;
  }

  @Override
  public boolean isInteraction() {
    return true;
  }

  public CahootsInteraction getInteraction() {
    return interaction;
  }
}
