package com.samourai.soroban.client.wallet;

import com.samourai.soroban.client.SorobanConfig;
import com.samourai.soroban.client.SorobanService;
import com.samourai.soroban.client.cahoots.OnlineCahootsService;
import com.samourai.soroban.client.meeting.SorobanMeetingService;
import com.samourai.soroban.client.protocol.SorobanProtocolMeeting;
import com.samourai.soroban.client.wallet.counterparty.SorobanWalletCounterparty;
import com.samourai.soroban.client.wallet.sender.SorobanWalletInitiator;
import com.samourai.wallet.cahoots.CahootsWallet;
import com.samourai.wallet.cahoots.manual.ManualCahootsService;
import com.samourai.wallet.cahoots.multi.MultiCahootsService;
import com.samourai.wallet.cahoots.stonewallx2.Stonewallx2Service;
import com.samourai.wallet.cahoots.stowaway.StowawayService;
import com.samourai.wallet.util.ExtLibJConfig;

public class SorobanWalletService {
  private SorobanProtocolMeeting sorobanProtocol;
  private OnlineCahootsService onlineCahootsService;
  private SorobanService sorobanService;
  private SorobanMeetingService sorobanMeetingService;
  private ManualCahootsService manualCahootsService;

  public SorobanWalletService(SorobanConfig sorobanConfig) {
    ExtLibJConfig extLibJConfig = sorobanConfig.getExtLibJConfig();
    StowawayService stowawayService = new StowawayService(extLibJConfig);
    Stonewallx2Service stonewallx2Service = new Stonewallx2Service(extLibJConfig);
    MultiCahootsService multiCahootsService =
        new MultiCahootsService(extLibJConfig, stonewallx2Service, stowawayService);

    this.sorobanProtocol = new SorobanProtocolMeeting(sorobanConfig);
    this.onlineCahootsService =
        new OnlineCahootsService(stowawayService, stonewallx2Service, multiCahootsService);
    this.sorobanService = new SorobanService(sorobanConfig, sorobanProtocol);
    this.sorobanMeetingService = new SorobanMeetingService();
    this.manualCahootsService =
        new ManualCahootsService(stowawayService, stonewallx2Service, multiCahootsService);
  }

  public SorobanWalletInitiator getSorobanWalletInitiator(CahootsWallet cahootsWallet) {
    return new SorobanWalletInitiator(
        onlineCahootsService, sorobanService, sorobanMeetingService, cahootsWallet);
  }

  public SorobanWalletCounterparty getSorobanWalletCounterparty(CahootsWallet cahootsWallet) {
    return new SorobanWalletCounterparty(
        onlineCahootsService, sorobanService, sorobanMeetingService, cahootsWallet);
  }

  public SorobanProtocolMeeting getSorobanProtocol() {
    return sorobanProtocol;
  }

  public SorobanService getSorobanService() {
    return sorobanService;
  }

  public OnlineCahootsService getOnlineCahootsService() {
    return onlineCahootsService;
  }

  public SorobanMeetingService getSorobanMeetingService() {
    return sorobanMeetingService;
  }

  public ManualCahootsService getManualCahootsService() {
    return manualCahootsService;
  }
}
