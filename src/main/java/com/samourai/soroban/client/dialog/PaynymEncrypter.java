package com.samourai.soroban.client.dialog;

import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.crypto.CryptoUtil;
import com.samourai.wallet.crypto.impl.ECDHKeySet;
import java.security.Provider;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaynymEncrypter implements Encrypter {
  private static final Logger log = LoggerFactory.getLogger(PaynymEncrypter.class);

  private ECKey myKey;
  private NetworkParameters params;
  private CryptoUtil cryptoUtil;

  public PaynymEncrypter(
      ECKey notificationAddressKey, NetworkParameters params, Provider provider) {
    this.myKey = notificationAddressKey;
    this.params = params;
    this.cryptoUtil = CryptoUtil.getInstance(provider);
  }

  @Override
  public String decrypt(byte[] encrypted, PaymentCode paymentCodePartner) throws Exception {
    ECKey partnerKey = getPartnerKey(paymentCodePartner);
    ECDHKeySet sharedSecret = cryptoUtil.getSharedSecret(myKey, partnerKey);
    return cryptoUtil.decryptString(encrypted, sharedSecret);
  }

  @Override
  public byte[] encrypt(String payload, PaymentCode paymentCodePartner) throws Exception {
    ECKey partnerKey = getPartnerKey(paymentCodePartner);
    ECDHKeySet sharedSecret = cryptoUtil.getSharedSecret(myKey, partnerKey);
    return cryptoUtil.encrypt(payload.getBytes(), sharedSecret);
  }

  protected ECKey getPartnerKey(PaymentCode paymentCodePartner) {
    return paymentCodePartner.notificationAddress(params).getECKey();
  }
}
