package com.samourai.soroban.client.wallet;

import com.samourai.soroban.cahoots.ManualCahootsService;
import com.samourai.soroban.client.SorobanService;
import com.samourai.soroban.client.cahoots.OnlineCahootsService;
import com.samourai.soroban.client.meeting.SorobanMeetingService;
import com.samourai.soroban.client.rpc.RpcClientService;
import com.samourai.soroban.client.wallet.counterparty.SorobanWalletCounterparty;
import com.samourai.soroban.client.wallet.sender.SorobanWalletInitiator;
import com.samourai.wallet.bip47.BIP47UtilGeneric;
import com.samourai.wallet.bipFormat.BipFormatSupplier;
import com.samourai.wallet.cahoots.CahootsWallet;
import com.samourai.wallet.cahoots.multi.MultiCahootsService;
import com.samourai.wallet.cahoots.stonewallx2.Stonewallx2Service;
import com.samourai.wallet.cahoots.stowaway.StowawayService;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanWalletService {
  private final Logger log = LoggerFactory.getLogger(SorobanWalletService.class);

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

    this.onlineCahootsService =
        new OnlineCahootsService(stowawayService, stonewallx2Service, multiCahootsService);
    this.sorobanService = new SorobanService(bip47Util, params, rpcClientService);
    this.sorobanMeetingService = new SorobanMeetingService(rpcClientService);
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
