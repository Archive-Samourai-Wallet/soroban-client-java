package com.samourai.soroban.client;

import com.samourai.http.client.IHttpClient;
import com.samourai.http.client.JavaHttpClient;
import com.samourai.soroban.client.cahoots.OnlineCahootsMessage;
import com.samourai.soroban.client.cahoots.SorobanCahootsService;
import com.samourai.soroban.client.meeting.SorobanMeetingService;
import com.samourai.soroban.client.rpc.RpcService;
import com.samourai.soroban.utils.LogbackUtils;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.bip47.rpc.java.Bip47UtilJava;
import com.samourai.wallet.bipFormat.BIP_FORMAT;
import com.samourai.wallet.bipWallet.WalletSupplier;
import com.samourai.wallet.bipWallet.WalletSupplierImpl;
import com.samourai.wallet.cahoots.CahootsTestUtil;
import com.samourai.wallet.cahoots.CahootsWallet;
import com.samourai.wallet.client.indexHandler.MemoryIndexHandlerSupplier;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactoryGeneric;
import com.samourai.wallet.send.provider.MockUtxoProvider;
import com.samourai.wallet.send.provider.SimpleCahootsUtxoProvider;
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
  protected static final HD_WalletFactoryGeneric hdWalletFactory =
      HD_WalletFactoryGeneric.getInstance();

  protected IHttpClient httpClient = new JavaHttpClient(TIMEOUT_MS);
  protected RpcService rpcService = new RpcService(httpClient, PROVIDER_JAVA, false);
  protected SorobanCahootsService sorobanCahootsService =
      new SorobanCahootsService(bip47Util, BIP_FORMAT.PROVIDER, params, rpcService);
  protected SorobanMeetingService sorobanMeetingService =
      sorobanCahootsService.getSorobanMeetingService();
  protected SorobanService sorobanService = sorobanCahootsService.getSorobanService();

  protected CahootsWallet cahootsWalletInitiator;
  protected CahootsWallet cahootsWalletCounterparty;

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
            BIP_FORMAT.PROVIDER,
            params,
            new SimpleCahootsUtxoProvider(utxoProviderInitiator));

    final HD_Wallet bip84WalletCounterparty =
        computeBip84wallet(SEED_WORDS, SEED_PASSPHRASE_COUNTERPARTY);
    WalletSupplier walletSupplierCounterparty =
        new WalletSupplierImpl(new MemoryIndexHandlerSupplier(), bip84WalletCounterparty);
    utxoProviderCounterparty = new MockUtxoProvider(params, walletSupplierCounterparty);
    cahootsWalletCounterparty =
        new CahootsWallet(
            walletSupplierCounterparty,
            BIP_FORMAT.PROVIDER,
            params,
            new SimpleCahootsUtxoProvider(utxoProviderCounterparty));

    paymentCodeInitiator = cahootsWalletInitiator.getPaymentCode();
    paymentCodeCounterparty = cahootsWalletCounterparty.getPaymentCode();
  }

  protected static void assertNoException() {
    if (exception != null) {
      Assertions.fail(exception);
    }
  }

  public static void setException(Exception e) {
    AbstractTest.exception = e;
    log.error("", e);
  }

  protected void verify(String expectedPayload, SorobanMessage message) throws Exception {
    OnlineCahootsMessage onlineCahootsMessage = (OnlineCahootsMessage) message;
    CahootsTestUtil.cleanPayload(onlineCahootsMessage.getCahoots());
    String payloadStr = onlineCahootsMessage.toPayload();
    Assertions.assertEquals(expectedPayload, payloadStr);
  }

  private static HD_Wallet computeBip84wallet(String seedWords, String passphrase)
      throws Exception {
    byte[] seed = hdWalletFactory.computeSeedFromWords(seedWords);
    HD_Wallet bip84w = hdWalletFactory.getBIP84(seed, passphrase, params);
    return bip84w;
  }
}
