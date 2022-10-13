package com.samourai.soroban.client.wallet;

import com.samourai.soroban.client.SorobanService;
import com.samourai.soroban.client.cahoots.OnlineCahootsService;
import com.samourai.soroban.client.meeting.SorobanMeetingService;
import com.samourai.wallet.cahoots.CahootsWallet;
import com.samourai.wallet.util.AsyncUtil;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SorobanWallet {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  protected static final AsyncUtil asyncUtil = AsyncUtil.getInstance();

  protected OnlineCahootsService onlineCahootsService;
  protected SorobanService sorobanService;
  protected SorobanMeetingService sorobanMeetingService;
  protected CahootsWallet cahootsWallet;
  protected int timeoutMeetingMs = 120000;
  protected int timeoutDialogMs = 60000;

  public SorobanWallet(
      OnlineCahootsService onlineCahootsService,
      SorobanService sorobanService,
      SorobanMeetingService sorobanMeetingService,
      CahootsWallet cahootsWallet) {
    this.onlineCahootsService = onlineCahootsService;
    this.sorobanService = sorobanService;
    this.sorobanMeetingService = sorobanMeetingService;
    this.cahootsWallet = cahootsWallet;
  }

  public int getTimeoutMeetingMs() {
    return timeoutMeetingMs;
  }

  public void setTimeoutMeetingMs(int timeoutMeetingMs) {
    this.timeoutMeetingMs = timeoutMeetingMs;
  }

  public int getTimeoutDialogMs() {
    return timeoutDialogMs;
  }

  public void setTimeoutDialogMs(int timeoutDialogMs) {
    this.timeoutDialogMs = timeoutDialogMs;
  }

  public CahootsWallet getCahootsWallet() {
    return cahootsWallet;
  }

  public OnlineCahootsService getOnlineCahootsService() {
    return onlineCahootsService;
  }

  public SorobanService getSorobanService() {
    return sorobanService;
  }

  public SorobanMeetingService getSorobanMeetingService() {
    return sorobanMeetingService;
  }
}
