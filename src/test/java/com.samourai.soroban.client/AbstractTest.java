package com.samourai.soroban.client;

import com.samourai.soroban.client.cahoots.OnlineCahootsMessage;
import com.samourai.soroban.utils.LogbackUtils;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.bip47.rpc.java.Bip47UtilJava;
import com.samourai.wallet.cahoots.CahootsTestUtil;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactoryGeneric;
import java.security.Provider;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.TestNet3Params;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTest {
  private static final Logger log = LoggerFactory.getLogger(AbstractTest.class);
  protected static final int TIMEOUT_MS = 20000;
  protected static final Provider PROVIDER_JAVA = new BouncyCastleProvider();

  protected static final NetworkParameters params = TestNet3Params.get();
  protected static final Bip47UtilJava bip47Util = Bip47UtilJava.getInstance();
  protected static final HD_WalletFactoryGeneric hdWalletFactory =
      HD_WalletFactoryGeneric.getInstance();

  private static volatile Exception exception = null;

  public AbstractTest() {
    LogbackUtils.setLogLevel("root", "INFO");
    LogbackUtils.setLogLevel("com.samourai", "DEBUG");
  }

  protected BIP47Wallet bip47Wallet(String seedWords, String passphrase) throws Exception {
    HD_Wallet hdWallet = hdWalletFactory.restoreWallet(seedWords, passphrase, params);
    BIP47Wallet bip47w =
        hdWalletFactory.getBIP47(hdWallet.getSeedHex(), hdWallet.getPassphrase(), params);
    return bip47w;
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
    log.info("### payload=" + payloadStr);
    Assertions.assertEquals(expectedPayload, payloadStr);
  }
}
