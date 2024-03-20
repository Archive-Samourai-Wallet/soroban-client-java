package com.samourai.soroban.client.wallet;

import com.samourai.soroban.client.SorobanService;
import com.samourai.soroban.client.cahoots.OnlineCahootsService;
import com.samourai.soroban.client.meeting.SorobanMeetingService;
import com.samourai.soroban.client.protocol.SorobanProtocolMeeting;
import com.samourai.soroban.client.rpc.RpcClientService;
import com.samourai.soroban.client.wallet.counterparty.SorobanWalletCounterparty;
import com.samourai.soroban.client.wallet.sender.SorobanWalletInitiator;
import com.samourai.wallet.bip47.BIP47UtilGeneric;
import com.samourai.wallet.bipFormat.BipFormatSupplier;
import com.samourai.wallet.cahoots.CahootsWallet;
import com.samourai.wallet.cahoots.manual.ManualCahootsService;
import com.samourai.wallet.cahoots.multi.MultiCahootsService;
import com.samourai.wallet.cahoots.stonewallx2.Stonewallx2Service;
import com.samourai.wallet.cahoots.stowaway.StowawayService;
import com.samourai.wallet.util.ExtLibJConfig;
import org.bitcoinj.core.NetworkParameters;

public class SorobanWalletService {
  private SorobanProtocolMeeting sorobanProtocol;
  private OnlineCahootsService onlineCahootsService;
  private SorobanService sorobanService;
  private SorobanMeetingService sorobanMeetingService;
  private ManualCahootsService manualCahootsService;

  public SorobanWalletService(
      BIP47UtilGeneric bip47Util,
      BipFormatSupplier bipFormatSupplier,
      NetworkParameters params,
      RpcClientService rpcClientService) {
    StowawayService stowawayService = new StowawayService(bipFormatSupplier, params);
    Stonewallx2Service stonewallx2Service = new Stonewallx2Service(bipFormatSupplier, params);
    MultiCahootsService multiCahootsService =
        new MultiCahootsService(bipFormatSupplier, params, stonewallx2Service, stowawayService);

    this.sorobanProtocol = new SorobanProtocolMeeting();
    this.onlineCahootsService =
        new OnlineCahootsService(stowawayService, stonewallx2Service, multiCahootsService);
    this.sorobanService = new SorobanService(bip47Util, params, rpcClientService, sorobanProtocol);
    this.sorobanMeetingService = new SorobanMeetingService();
    this.manualCahootsService =
        new ManualCahootsService(stowawayService, stonewallx2Service, multiCahootsService);
  }

  public SorobanWalletService(ExtLibJConfig extLibJConfig, RpcClientService rpcClientService) {
    this(
        extLibJConfig.getBip47Util(),
        extLibJConfig.getBipFormatSupplier(),
        extLibJConfig.getSamouraiNetwork().getParams(),
        rpcClientService);
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
