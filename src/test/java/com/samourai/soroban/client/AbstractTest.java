package com.samourai.soroban.client;

import com.samourai.http.client.JavaHttpClient;
import com.samourai.soroban.client.meeting.SorobanMeetingService;
import com.samourai.soroban.client.rpc.RpcService;
import com.samourai.soroban.client.wallet.SorobanWalletService;
import com.samourai.soroban.client.wallet.counterparty.SorobanWalletCounterparty;
import com.samourai.soroban.client.wallet.sender.SorobanWalletInitiator;
import com.samourai.soroban.utils.LogbackUtils;
import com.samourai.wallet.api.backend.beans.WalletResponse;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.bip47.rpc.java.Bip47UtilJava;
import com.samourai.wallet.bipFormat.BIP_FORMAT;
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
import java.security.Provider;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.TestNet3Params;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTest {
  private static final Logger log = LoggerFactory.getLogger(AbstractTest.class);

  protected static final String SEED_WORDS = "all all all all all all all all all all all all";
  protected static final String SEED_PASSPHRASE_INITIATOR = "initiator";
  protected static final String SEED_PASSPHRASE_COUNTERPARTY = "counterparty";

  protected static final int TIMEOUT_MS = 20000;
  protected static final Provider PROVIDER_JAVA = new BouncyCastleProvider();

  protected static final NetworkParameters params = TestNet3Params.get();
  protected static final Bip47UtilJava bip47Util = Bip47UtilJava.getInstance();

  protected static final ChainSupplier chainSupplier = () -> {
    WalletResponse.InfoBlock infoBlock = new WalletResponse.InfoBlock();
    infoBlock.height = 1234;
    return infoBlock;
  };

  protected static final HD_WalletFactoryGeneric hdWalletFactory =
      HD_WalletFactoryGeneric.getInstance();
  protected static final AsyncUtil asyncUtil = AsyncUtil.getInstance();

  protected JavaHttpClient httpClient = new JavaHttpClient(TIMEOUT_MS);
  protected CryptoUtil cryptoUtil = CryptoUtil.getInstance(PROVIDER_JAVA);
  protected RpcService rpcService = new RpcService(httpClient, cryptoUtil, false);
  protected SorobanWalletService sorobanWalletService =
      new SorobanWalletService(bip47Util, BIP_FORMAT.PROVIDER, params, rpcService);
  protected SorobanMeetingService sorobanMeetingService =
      sorobanWalletService.getSorobanMeetingService();
  protected SorobanService sorobanService = sorobanWalletService.getSorobanService();

  protected CahootsWallet cahootsWalletInitiator;
  protected CahootsWallet cahootsWalletCounterparty;
  protected SorobanWalletInitiator sorobanWalletInitiator;
  protected SorobanWalletCounterparty sorobanWalletCounterparty;

  protected MockUtxoProvider utxoProviderInitiator;
  protected MockUtxoProvider utxoProviderCounterparty;

  protected PaymentCode paymentCodeInitiator;
  protected PaymentCode paymentCodeCounterparty;

  private static volatile Exception exception = null;

  public AbstractTest() {
    LogbackUtils.setLogLevel("root", "INFO");
    LogbackUtils.setLogLevel("com.samourai", "DEBUG");
  }

  public void setUp() throws Exception {
    final HD_Wallet bip84WalletSender = computeBip84wallet(SEED_WORDS, SEED_PASSPHRASE_INITIATOR);
    WalletSupplier walletSupplierSender =
        new WalletSupplierImpl(new MemoryIndexHandlerSupplier(), bip84WalletSender);
    utxoProviderInitiator = new MockUtxoProvider(params, walletSupplierSender);
    cahootsWalletInitiator =
        new CahootsWallet(
            walletSupplierSender,
            chainSupplier,
            BIP_FORMAT.PROVIDER,
            params,
            new SimpleCahootsUtxoProvider(utxoProviderInitiator));
    sorobanWalletInitiator = sorobanWalletService.getSorobanWalletInitiator(cahootsWalletInitiator);

    final HD_Wallet bip84WalletCounterparty =
        computeBip84wallet(SEED_WORDS, SEED_PASSPHRASE_COUNTERPARTY);
    WalletSupplier walletSupplierCounterparty =
        new WalletSupplierImpl(new MemoryIndexHandlerSupplier(), bip84WalletCounterparty);
    utxoProviderCounterparty = new MockUtxoProvider(params, walletSupplierCounterparty);
    cahootsWalletCounterparty =
        new CahootsWallet(
            walletSupplierCounterparty,
            chainSupplier,
            BIP_FORMAT.PROVIDER,
            params,
            new SimpleCahootsUtxoProvider(utxoProviderCounterparty));
    sorobanWalletCounterparty =
        sorobanWalletService.getSorobanWalletCounterparty(cahootsWalletCounterparty);

    paymentCodeInitiator = cahootsWalletInitiator.getPaymentCode();
    paymentCodeCounterparty = cahootsWalletCounterparty.getPaymentCode();

    httpClient.getJettyHttpClient().start();
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

  private static HD_Wallet computeBip84wallet(String seedWords, String passphrase)
      throws Exception {
    byte[] seed = hdWalletFactory.computeSeedFromWords(seedWords);
    HD_Wallet bip84w = hdWalletFactory.getBIP84(seed, passphrase, params);
    return bip84w;
  }
}
