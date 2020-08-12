package com.samourai.soroban.client.dialog;

import org.bitcoinj.core.ECKey;
import org.bouncycastle.util.encoders.Hex;

public class User {
  ECKey privateKey;

  public User(ECKey privateKey) {
    this.privateKey = privateKey;
  }

  public String publicKey() {
    return this.privateKey.getPublicKeyAsHex();
  }

  public Box box(byte[] otherPublicKey) {
    return new Box(privateKey.getPrivKeyBytes(), otherPublicKey);
  }

  public String sharedSecret(Box box) {
    byte[] sharedSecret = box.sharedSecret();
    return Hex.toHexString(sharedSecret);
  }
}
