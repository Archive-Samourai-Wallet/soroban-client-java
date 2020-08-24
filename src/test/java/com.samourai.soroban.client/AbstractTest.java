package com.samourai.soroban.client;

import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.bip47.rpc.java.Bip47UtilJava;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.java.HD_WalletFactoryJava;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.TestNet3Params;

public abstract class AbstractTest {
  protected static final int SOROBAN_TIMEOUT_MS = 2000;

  protected static final NetworkParameters params = TestNet3Params.get();
  protected static final Bip47UtilJava bip47Util = Bip47UtilJava.getInstance();
  protected static final HD_WalletFactoryJava hdWalletFactory = HD_WalletFactoryJava.getInstance();

  protected BIP47Wallet bip47Wallet(String seedWords, String passphrase) throws Exception {
    HD_Wallet hdWallet = hdWalletFactory.restoreWallet(seedWords, passphrase, 1, params);
    BIP47Wallet bip47w =
        hdWalletFactory.getBIP47(hdWallet.getSeedHex(), hdWallet.getPassphrase(), params);
    return bip47w;
  }
}
