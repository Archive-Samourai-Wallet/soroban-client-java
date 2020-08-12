package com.samourai.soroban.client.dialog;

import com.codahale.xsalsa20poly1305.Keys;
import com.codahale.xsalsa20poly1305.SecretBox;
import com.google.common.base.Charsets;
import java.util.Arrays;
import org.bouncycastle.util.encoders.Hex;

public class Box extends SecretBox {
  static final int NONCE_SIZE = 24;

  private byte[] privateKey;
  private byte[] otherPublicKey;

  public Box(byte[] privateKey, byte[] otherPublicKey) {
    super(otherPublicKey, privateKey);
    this.privateKey = privateKey;
    this.otherPublicKey = otherPublicKey;
  }

  public byte[] sharedSecret() {
    return Keys.sharedSecret(otherPublicKey, privateKey);
  }

  public String encrypt(String message) throws Exception {
    byte[] data = message.getBytes(Charsets.UTF_8);
    byte[] nonce = super.nonce(data);
    data = super.seal(nonce, data);

    byte[] ret = new byte[nonce.length + data.length];
    System.arraycopy(nonce, 0, ret, 0, nonce.length);
    System.arraycopy(data, 0, ret, nonce.length, data.length);

    String payload = Hex.toHexString(ret);
    if (payload.isEmpty()) {
      throw new Exception("Invalid query");
    }
    return payload;
  }

  public String decrypt(String message) throws Exception {
    byte[] data = Hex.decode(message);
    byte[] nonce = Arrays.copyOfRange(data, 0, NONCE_SIZE);
    byte[] ciphertext = Arrays.copyOfRange(data, NONCE_SIZE, data.length);
    String result = new String(super.open(nonce, ciphertext).get(), Charsets.UTF_8);
    if (result.isEmpty()) {
      throw new Exception("Invalid reponse message");
    }
    return result;
  }
}
