package com.samourai.soroban.client;

import com.samourai.http.client.JettyHttpClient;
import com.samourai.soroban.client.endpoint.SorobanApp;
import com.samourai.soroban.client.endpoint.SorobanEndpoint;
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
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.bip47.rpc.java.Bip47UtilJava;
import com.samourai.wallet.bipFormat.BIP_FORMAT;
import com.samourai.wallet.bipFormat.BipFormatSupplier;
import com.samourai.wallet.bipWallet.WalletSupplier;
import com.samourai.wallet.bipWallet.WalletSupplierImpl;
import com.samourai.wallet.cahoots.CahootsWallet;
import com.samourai.wallet.cahoots.CahootsWalletImpl;
import com.samourai.wallet.chain.ChainSupplier;
import com.samourai.wallet.client.indexHandler.MemoryIndexHandlerSupplier;
import com.samourai.wallet.constants.WhirlpoolNetwork;
import com.samourai.wallet.crypto.CryptoUtil;
import com.samourai.wallet.dexConfig.DexConfigProvider;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactoryGeneric;
import com.samourai.wallet.httpClient.HttpUsage;
import com.samourai.wallet.httpClient.IHttpClient;
import com.samourai.wallet.httpClient.IHttpClientService;
import com.samourai.wallet.send.provider.MockUtxoProvider;
import com.samourai.wallet.send.provider.SimpleCahootsUtxoProvider;
import com.samourai.wallet.sorobanClient.RpcWallet;
import com.samourai.wallet.util.AsyncUtil;
import com.samourai.wallet.util.MessageSignUtilGeneric;
import java.security.Provider;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiPredicate;
import org.bitcoinj.core.ECKey;
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
  protected static final MessageSignUtilGeneric messageSignUtil =
      MessageSignUtilGeneric.getInstance();

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

  protected JettyHttpClient httpClient = new JettyHttpClient(TIMEOUT_MS, null, "soroban-test");
  protected IHttpClientService httpClientService =
      new IHttpClientService() {
        @Override
        public IHttpClient getHttpClient(HttpUsage httpUsage) {
          return httpClient;
        }

        @Override
        public void changeIdentity() {}

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
  protected ECKey samouraiSigningKey;
  protected String samouraiSigningAddress;

  private static volatile Exception exception = null;

  public AbstractTest() {
    LogbackUtils.setLogLevel("root", "INFO");
    LogbackUtils.setLogLevel("com.samourai", "DEBUG");
  }

  public void setUp() throws Exception {
    if (app != null) {
      // exit previous sessions
      rpcSessionInitiator.exit();
      rpcSessionCounterparty.exit();
    }

    final HD_Wallet bip84WalletSender = computeBip84wallet(SEED_WORDS, SEED_PASSPHRASE_INITIATOR);
    WalletSupplier walletSupplierSender =
        new WalletSupplierImpl(
            bipFormatSupplier, new MemoryIndexHandlerSupplier(), bip84WalletSender);
    utxoProviderInitiator = new MockUtxoProvider(params, walletSupplierSender);
    cahootsWalletInitiator =
        new CahootsWalletImpl(
            chainSupplier,
            walletSupplierSender,
            new SimpleCahootsUtxoProvider(utxoProviderInitiator));
    sorobanWalletInitiator = sorobanWalletService.getSorobanWalletInitiator(cahootsWalletInitiator);

    final HD_Wallet bip84WalletCounterparty =
        computeBip84wallet(SEED_WORDS, SEED_PASSPHRASE_COUNTERPARTY);
    WalletSupplier walletSupplierCounterparty =
        new WalletSupplierImpl(
            bipFormatSupplier, new MemoryIndexHandlerSupplier(), bip84WalletCounterparty);
    utxoProviderCounterparty = new MockUtxoProvider(params, walletSupplierCounterparty);
    cahootsWalletCounterparty =
        new CahootsWalletImpl(
            chainSupplier,
            walletSupplierCounterparty,
            new SimpleCahootsUtxoProvider(utxoProviderCounterparty));
    sorobanWalletCounterparty =
        sorobanWalletService.getSorobanWalletCounterparty(cahootsWalletCounterparty);

    paymentCodeInitiator = cahootsWalletInitiator.getBip47Account().getPaymentCode();
    paymentCodeCounterparty = cahootsWalletCounterparty.getBip47Account().getPaymentCode();

    httpClient.getJettyHttpClient().start();

    initialSorobanServerTestnetClearUrls =
        DexConfigProvider.getInstance().getSamouraiConfig().getSorobanServerDexTestnetClear();
    // only keep 1 SorobanServerDex to avoid RPC propagation delay
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

    samouraiSigningKey = new ECKey();
    samouraiSigningAddress = samouraiSigningKey.toAddress(params).toString();
    whirlpoolNetwork._setSigningAddress(samouraiSigningAddress);
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

  protected <I, S> void doTestEndpointReply(
      SorobanEndpoint<I, S, ?> endpointInitiator,
      SorobanEndpoint<I, S, ?> endpointCounterparty,
      S payload,
      S responsePayload,
      BiPredicate<S, I> equals)
      throws Exception {

    // send payload
    I request =
        asyncUtil.blockingGet(
            rpcSessionInitiator.withSorobanClient(
                sorobanClient -> endpointInitiator.sendSingle(sorobanClient, payload)));
    waitSorobanDelay();

    // get payload
    rpcSessionCounterparty.withSorobanClient(
        sorobanClient -> {
          // get payload
          I result = asyncUtil.blockingGet(endpointCounterparty.findAny(sorobanClient)).get();
          Assertions.assertTrue(equals.test(payload, result));

          // send reply
          Bip47Encrypter encrypter = rpcSessionCounterparty.getRpcWallet().getBip47Encrypter();
          SorobanEndpoint<I, S, ?> endpointReply =
              endpointCounterparty.getEndpointReply(result, encrypter);
          asyncUtil.blockingAwait(endpointReply.send(sorobanClient, responsePayload));
          return result;
        });
    waitSorobanDelay();
    waitSorobanDelay();

    // get reply
    rpcSessionInitiator.withSorobanClient(
        sorobanClient -> {
          // get reply
          Bip47Encrypter encrypter = rpcSessionInitiator.getRpcWallet().getBip47Encrypter();
          SorobanEndpoint<I, S, ?> endpointReply =
              endpointInitiator.getEndpointReply(request, encrypter);
          I result = asyncUtil.blockingGet(endpointReply.findAny(sorobanClient)).get();
          Assertions.assertTrue(equals.test(responsePayload, result));
          return result;
        });

    // payloads cleared
    waitSorobanDelay();
    waitSorobanDelay();
    Assertions.assertEquals(
        0,
        asyncUtil
            .blockingGet(
                rpcSessionInitiator.withSorobanClient(
                    sorobanClient -> endpointInitiator.getList(sorobanClient)))
            .size());
  }

  protected <I, S> void doTestEndpoint(
      SorobanEndpoint<I, S, ?> endpointInitiator,
      SorobanEndpoint<I, S, ?> endpointCounterparty,
      S payload,
      BiPredicate<S, I> equals)
      throws Exception {

    // send payload
    asyncUtil.blockingGet(
        rpcSessionInitiator.withSorobanClient(
            sorobanClient -> endpointInitiator.sendSingle(sorobanClient, payload)));
    waitSorobanDelay();

    // get payload
    rpcSessionCounterparty.withSorobanClient(
        sorobanClient -> {
          // get payload
          I result = asyncUtil.blockingGet(endpointCounterparty.findAny(sorobanClient)).get();
          Assertions.assertTrue(equals.test(payload, result));
          return result;
        });

    // payloads cleared
    waitSorobanDelay();
    waitSorobanDelay();
    Assertions.assertEquals(
        0,
        asyncUtil
            .blockingGet(
                rpcSessionInitiator.withSorobanClient(
                    sorobanClient -> endpointInitiator.getList(sorobanClient)))
            .size());
  }

  protected <I, S> void doTestEndpointSkippedPayload(
      SorobanEndpoint<I, S, ?> endpointInitiator,
      SorobanEndpoint<I, S, ?> endpointCounterparty,
      S payload)
      throws Exception {
    // send payload
    asyncUtil.blockingAwait(
        rpcSessionInitiator.withSorobanClient(
            sorobanClient -> endpointInitiator.send(sorobanClient, payload)));
    waitSorobanDelay();

    // get payload - nothing received
    Assertions.assertFalse(
        asyncUtil
            .blockingGet(
                rpcSessionCounterparty.withSorobanClient(
                    sorobanClient -> endpointCounterparty.findAny(sorobanClient)))
            .isPresent());
  }

  protected <I, S> void doTestEndpoint2WaysList(
      SorobanEndpoint<I, S, ?> endpointInitiator,
      SorobanEndpoint<I, S, ?> endpointCounterparty,
      S[] payloads)
      throws Exception {

    // send payloads
    rpcSessionInitiator.withSorobanClient(
        sorobanClient -> {
          for (S payload : payloads) {
            asyncUtil.blockingAwait(endpointInitiator.send(sorobanClient, payload));
          }
          return null;
        });

    // get all payloads
    waitSorobanDelay();
    Assertions.assertEquals(
        payloads.length,
        asyncUtil
            .blockingGet(
                rpcSessionCounterparty.withSorobanClient(
                    sorobanClient -> endpointCounterparty.getList(sorobanClient)))
            .size());

    // payloads cleared
    waitSorobanDelay();
    Assertions.assertEquals(
        0,
        asyncUtil
            .blockingGet(
                rpcSessionInitiator.withSorobanClient(
                    sorobanClient -> endpointInitiator.getList(sorobanClient)))
            .size());
  }

  protected <I, S> void doTestEndpointDelete(
      SorobanEndpoint<I, S, ?> endpointInitiator,
      SorobanEndpoint<I, S, ?> endpointCounterparty,
      S payload1,
      S payload2)
      throws Exception {

    // send payloads
    asyncUtil.blockingAwait(
        rpcSessionInitiator.withSorobanClient(
            sorobanClient -> endpointInitiator.send(sorobanClient, payload1)));
    asyncUtil.blockingAwait(
        rpcSessionInitiator.withSorobanClient(
            sorobanClient -> endpointInitiator.send(sorobanClient, payload2)));

    waitSorobanDelay();

    // list
    List<I> result =
        asyncUtil.blockingGet(
            rpcSessionCounterparty.withSorobanClient(
                sorobanClient -> endpointCounterparty.getList(sorobanClient)));
    Assertions.assertEquals(2, result.size());

    // delete 1
    asyncUtil.blockingAwait(
        rpcSessionCounterparty.withSorobanClient(
            sorobanClient -> endpointCounterparty.remove(sorobanClient, result.iterator().next())));
    waitSorobanDelay();

    // list
    List<I> resultNew =
        asyncUtil.blockingGet(
            rpcSessionCounterparty.withSorobanClient(
                sorobanClient -> endpointCounterparty.getList(sorobanClient)));
    Assertions.assertEquals(1, resultNew.size());
  }
}
