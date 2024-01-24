package com.samourai.soroban.client;

import com.samourai.dex.config.DexConfigProvider;
import com.samourai.http.client.HttpUsage;
import com.samourai.http.client.IHttpClient;
import com.samourai.http.client.IHttpClientService;
import com.samourai.http.client.JavaHttpClient;
import com.samourai.soroban.client.endpoint.SorobanApp;
import com.samourai.soroban.client.meeting.SorobanMeetingService;
import com.samourai.soroban.client.protocol.SorobanProtocolMeeting;
import com.samourai.soroban.client.rpc.RpcClientService;
import com.samourai.soroban.client.rpc.RpcSession;
import com.samourai.soroban.client.rpc.RpcWalletImpl;
import com.samourai.soroban.client.wallet.SorobanWalletService;
import com.samourai.soroban.client.wallet.counterparty.SorobanWalletCounterparty;
import com.samourai.soroban.client.wallet.sender.SorobanWalletInitiator;
import com.samourai.soroban.utils.LogbackUtils;
import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.bip47.rpc.BIP47Account;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.bip47.rpc.java.Bip47UtilJava;
import com.samourai.wallet.bipFormat.BIP_FORMAT;
import com.samourai.wallet.bipFormat.BipFormatSupplier;
import com.samourai.wallet.bipWallet.WalletSupplier;
import com.samourai.wallet.bipWallet.WalletSupplierImpl;
import com.samourai.wallet.cahoots.CahootsWallet;
import com.samourai.wallet.chain.ChainSupplier;
import com.samourai.wallet.client.indexHandler.MemoryIndexHandlerSupplier;
import com.samourai.wallet.crypto.CryptoUtil;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactoryGeneric;
import com.samourai.wallet.send.provider.MockUtxoProvider;
import com.samourai.wallet.send.provider.SimpleCahootsUtxoProvider;
import com.samourai.wallet.util.AsyncUtil;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolNetwork;
import java.security.Provider;
import java.util.Arrays;
import java.util.Collection;
import org.bitcoinj.core.NetworkParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTest {
  private static final Logger log = LoggerFactory.getLogger(AbstractTest.class);

  protected static final String SEED_WORDS = "all all all all all all all all all all all all";
  protected static final String SEED_PASSPHRASE_INITIATOR = "initiator";
  protected static final String SEED_PASSPHRASE_COUNTERPARTY = "counterparty";

  protected static final int TIMEOUT_MS = 10000;
  protected static final Provider PROVIDER_JAVA = new BouncyCastleProvider();

  protected static final WhirlpoolNetwork whirlpoolNetwork = WhirlpoolNetwork.TESTNET;
  protected static final NetworkParameters params = whirlpoolNetwork.getParams();
  protected static final Bip47UtilJava bip47Util = Bip47UtilJava.getInstance();
  protected static final BipFormatSupplier bipFormatSupplier = BIP_FORMAT.PROVIDER;

  protected static final ChainSupplier chainSupplier =
      () -> {
        WalletResponse.InfoBlock infoBlock = new WalletResponse.InfoBlock();
        infoBlock.height = 1234;
        return infoBlock;
      };

  protected static final HD_WalletFactoryGeneric hdWalletFactory =
      HD_WalletFactoryGeneric.getInstance();
  protected static final AsyncUtil asyncUtil = AsyncUtil.getInstance();

  protected JavaHttpClient httpClient = new JavaHttpClient(TIMEOUT_MS);
  protected IHttpClientService httpClientService =
      new IHttpClientService() {
        @Override
        public IHttpClient getHttpClient(HttpUsage httpUsage) {
          return httpClient;
        }

        @Override
        public void stop() {}
      };
  protected CryptoUtil cryptoUtil = CryptoUtil.getInstance(PROVIDER_JAVA);
  protected RpcClientService rpcClientService =
      new RpcClientService(httpClientService, cryptoUtil, bip47Util, false, params);
  protected SorobanWalletService sorobanWalletService =
      new SorobanWalletService(bip47Util, BIP_FORMAT.PROVIDER, params, rpcClientService);
  protected SorobanProtocolMeeting sorobanProtocol = sorobanWalletService.getSorobanProtocol();
  protected SorobanMeetingService sorobanMeetingService =
      sorobanWalletService.getSorobanMeetingService();
  protected SorobanService sorobanService = sorobanWalletService.getSorobanService();

  protected CahootsWallet cahootsWalletInitiator;
  protected CahootsWallet cahootsWalletCounterparty;
  protected SorobanWalletInitiator sorobanWalletInitiator;
  protected SorobanWalletCounterparty sorobanWalletCounterparty;

  protected BIP47Account bip47AccountInitiator;
  protected RpcWallet rpcWalletInitiator;
  protected RpcSession rpcSessionInitiator;
  protected SorobanClient sorobanClientInitiator;

  protected BIP47Account bip47AccountCounterparty;
  protected RpcWallet rpcWalletCounterparty;
  protected RpcSession rpcSessionCounterparty;
  protected SorobanClient sorobanClientCounterparty;

  protected MockUtxoProvider utxoProviderInitiator;
  protected MockUtxoProvider utxoProviderCounterparty;

  protected PaymentCode paymentCodeInitiator;
  protected PaymentCode paymentCodeCounterparty;

  protected Collection<String> initialSorobanServerTestnetClearUrls;
  protected String appVersion;
  protected SorobanApp app;

  private static volatile Exception exception = null;

  public AbstractTest() {
    LogbackUtils.setLogLevel("root", "INFO");
    LogbackUtils.setLogLevel("com.samourai", "DEBUG");
  }

  public void setUp() throws Exception {
    final HD_Wallet bip84WalletSender = computeBip84wallet(SEED_WORDS, SEED_PASSPHRASE_INITIATOR);
    WalletSupplier walletSupplierSender =
        new WalletSupplierImpl(
            bipFormatSupplier, new MemoryIndexHandlerSupplier(), bip84WalletSender);
    utxoProviderInitiator = new MockUtxoProvider(params, walletSupplierSender);
    cahootsWalletInitiator =
        new CahootsWallet(
            walletSupplierSender,
            chainSupplier,
            BIP_FORMAT.PROVIDER,
            new SimpleCahootsUtxoProvider(utxoProviderInitiator));
    sorobanWalletInitiator = sorobanWalletService.getSorobanWalletInitiator(cahootsWalletInitiator);

    final HD_Wallet bip84WalletCounterparty =
        computeBip84wallet(SEED_WORDS, SEED_PASSPHRASE_COUNTERPARTY);
    WalletSupplier walletSupplierCounterparty =
        new WalletSupplierImpl(
            bipFormatSupplier, new MemoryIndexHandlerSupplier(), bip84WalletCounterparty);
    utxoProviderCounterparty = new MockUtxoProvider(params, walletSupplierCounterparty);
    cahootsWalletCounterparty =
        new CahootsWallet(
            walletSupplierCounterparty,
            chainSupplier,
            BIP_FORMAT.PROVIDER,
            new SimpleCahootsUtxoProvider(utxoProviderCounterparty));
    sorobanWalletCounterparty =
        sorobanWalletService.getSorobanWalletCounterparty(cahootsWalletCounterparty);

    paymentCodeInitiator = cahootsWalletInitiator.getBip47Account().getPaymentCode();
    paymentCodeCounterparty = cahootsWalletCounterparty.getBip47Account().getPaymentCode();

    httpClient.getJettyHttpClient().start();

    // only keep 1 SorobanServerDex to avoid RPC propagation delay
    initialSorobanServerTestnetClearUrls =
        DexConfigProvider.getInstance().getSamouraiConfig().getSorobanServerDexTestnetClear();
    DexConfigProvider.getInstance()
        .getSamouraiConfig()
        .setSorobanServerDexTestnetClear(
            Arrays.asList(initialSorobanServerTestnetClearUrls.iterator().next()));

    this.bip47AccountInitiator = cahootsWalletInitiator.getBip47Account();
    this.rpcWalletInitiator = rpcClientService.getRpcWallet(bip47AccountInitiator);
    this.rpcSessionInitiator = ((RpcWalletImpl) rpcWalletInitiator).createRpcSession();
    this.sorobanClientInitiator = rpcSessionInitiator.withSorobanClient(sc -> sc);

    this.bip47AccountCounterparty = cahootsWalletCounterparty.getBip47Account();
    this.rpcWalletCounterparty = rpcClientService.getRpcWallet(bip47AccountCounterparty);
    this.rpcSessionCounterparty = ((RpcWalletImpl) rpcWalletCounterparty).createRpcSession();
    this.sorobanClientCounterparty = rpcSessionCounterparty.withSorobanClient(sc -> sc);

    this.appVersion =
        "" + System.currentTimeMillis(); // change version to avoid conflicts between test runs
    this.app = new SorobanApp(whirlpoolNetwork, "APP_TEST", appVersion);
  }

  protected static void assertNoException() {
    if (exception != null) {
      Assertions.fail(exception);
    }
  }

  protected static void assertException(String msg) {
    Assertions.assertNotNull(exception);
    Assertions.assertEquals(msg, exception.getMessage());
  }

  public static void setException(Exception e) {
    AbstractTest.exception = e;
    log.error("", e);
  }

  protected static HD_Wallet computeBip84wallet(String seedWords, String passphrase)
      throws Exception {
    byte[] seed = hdWalletFactory.computeSeedFromWords(seedWords);
    HD_Wallet bip84w = hdWalletFactory.getBIP84(seed, passphrase, params);
    return bip84w;
  }

  protected void runDelayed(long delayMs, Runnable run) {
    new Thread(
            () -> {
              try {
                Thread.sleep(delayMs);
              } catch (InterruptedException e) {
              }
              run.run();
            })
        .start();
  }

  protected synchronized void waitSorobanDelay() {
    try {
      wait(2000);
    } catch (InterruptedException e) {
    }
  }
}
