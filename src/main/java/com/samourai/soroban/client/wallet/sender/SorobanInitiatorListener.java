package com.samourai.soroban.client.wallet.sender;

import com.samourai.soroban.client.OnlineSorobanInteraction;
import com.samourai.soroban.client.cahoots.OnlineCahootsMessage;
import com.samourai.soroban.client.meeting.SorobanResponseMessage;

public interface SorobanInitiatorListener {
  void onResponse(SorobanResponseMessage sorobanResponse) throws Exception;

  void progress(OnlineCahootsMessage message);

  void onInteraction(OnlineSorobanInteraction interaction) throws Exception;
}
