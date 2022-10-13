package com.samourai.soroban.client.wallet.sender;

import com.samourai.soroban.client.OnlineSorobanInteraction;
import com.samourai.soroban.client.cahoots.OnlineCahootsMessage;

public interface SorobanInitiatorListener {
  void progress(OnlineCahootsMessage message);

  void onInteraction(OnlineSorobanInteraction interaction) throws Exception;
}
