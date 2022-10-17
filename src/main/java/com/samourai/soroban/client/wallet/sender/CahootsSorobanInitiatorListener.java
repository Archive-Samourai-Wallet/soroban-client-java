package com.samourai.soroban.client.wallet.sender;

import com.samourai.soroban.client.OnlineSorobanInteraction;
import com.samourai.soroban.client.cahoots.OnlineCahootsMessage;
import com.samourai.soroban.client.meeting.SorobanResponseMessage;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CahootsSorobanInitiatorListener implements SorobanInitiatorListener {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public CahootsSorobanInitiatorListener() {}

  @Override
  public void onResponse(SorobanResponseMessage sorobanResponse) throws Exception {
    log.info(
        "(Soroban sender) meeting response: "
            + (sorobanResponse.isAccept() ? "accepted" : "declined"));
  }

  @Override
  public void progress(OnlineCahootsMessage message) {
    log.info("(Soroban sender) progress: " + message);
  }

  @Override
  public void onInteraction(OnlineSorobanInteraction interaction) throws Exception {
    // automatically confirm interactions by default (override it if needed)
    interaction.sorobanAccept();
  }
}
