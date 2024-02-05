package com.samourai.soroban.client.endpoint.meta.wrapper;

import com.samourai.soroban.client.endpoint.meta.SorobanMetadata;
import com.samourai.soroban.client.exception.SorobanException;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import com.samourai.wallet.util.MessageSignUtilGeneric;
import com.samourai.wallet.util.Pair;
import org.apache.commons.lang3.StringUtils;

/** Metadata: auth */
public abstract class AbstractSorobanWrapperMetaAuth implements SorobanWrapperMeta {
  private static final String KEY_AUTH = "auth";
  private static final MessageSignUtilGeneric messageSignUtil =
      MessageSignUtilGeneric.getInstance();

  private String myAuth;

  // for coordinator
  public AbstractSorobanWrapperMetaAuth(String myAuth) {
    this.myAuth = myAuth;
  }

  // for client
  public AbstractSorobanWrapperMetaAuth() {
    this(null);
  }

  protected abstract void validateAuth(
      Bip47Encrypter encrypter, Pair<String, SorobanMetadata> entry, String auth)
      throws SorobanException;

  @Override
  public Pair<String, SorobanMetadata> onSend(
      Bip47Encrypter encrypter, Pair<String, SorobanMetadata> entry, Object initialPayload)
      throws Exception {
    // set auth
    entry.getRight().setMeta(KEY_AUTH, myAuth);
    return entry;
  }

  @Override
  public Pair<String, SorobanMetadata> onReceive(
      Bip47Encrypter encrypter, Pair<String, SorobanMetadata> entry) throws Exception {
    // require auth
    String auth = getAuth(entry.getRight());
    if (StringUtils.isEmpty(auth)) {
      throw new SorobanException("Missing metadata.auth");
    }

    // validate auth
    validateAuth(encrypter, entry, auth);
    return entry;
  }

  public static String getAuth(SorobanMetadata metadata) {
    String auth = metadata.getMetaString(KEY_AUTH);
    if (StringUtils.isEmpty(auth)) {
      return null;
    }
    return auth;
  }
}
