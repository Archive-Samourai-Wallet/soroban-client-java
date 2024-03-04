package com.samourai.soroban.client.endpoint;

import com.samourai.soroban.client.endpoint.meta.SorobanFilter;
import com.samourai.soroban.client.endpoint.wrapper.SorobanWrapperString;
import com.samourai.soroban.client.rpc.RpcMode;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.util.Pair;

/**
 * This endpoint uses raw String as payload.
 *
 * <p>This endpoint has limited features:<br>
 * - only support SorobanWrapperString[]<br>
 * - .remove() is NOT supported when using SorobanWrapperEncrypt, use .removeRaw() instead
 */
public class SorobanEndpointRaw
    extends AbstractSorobanEndpoint<String, String, Void, SorobanFilter<String>> {
  public SorobanEndpointRaw(String dir, RpcMode rpcMode, SorobanWrapperString[] wrappers) {
    super(dir, rpcMode, wrappers);
  }

  @Override
  protected SorobanFilter<String> newFilterBuilder() {
    return null; // not supported yet
  }

  @Override
  protected String entryToRaw(Pair<String, Void> entry) throws Exception {
    return entry.getLeft();
  }

  @Override
  protected Pair<String, Void> rawToEntry(String rawEntry) throws Exception {
    return Pair.of(rawEntry, null);
  }

  @Override
  protected Pair<String, Void> newEntry(String payload) throws Exception {
    return Pair.of(payload, null);
  }

  @Override
  protected String newEntry(Pair<String, Void> entry, String rawEntry) {
    return entry.getLeft();
  }

  @Override
  protected String getRawEntry(String entry) {
    if (getEncryptTo() != null || getDecryptFrom() != null) {
      throw new RuntimeException(
          "getRawEntry() not available for SorobanEndpointRaw with encryption");
    }
    return entry;
  }

  @Override
  public SorobanEndpoint newEndpointReply(String request, Bip47Encrypter encrypter) {
    SorobanEndpointRaw endpoint =
        new SorobanEndpointRaw(getDir(), getReplyRpcMode(), new SorobanWrapperString[] {});
    endpoint.setEncryptReply(this, request, encrypter);
    return endpoint;
  }

  @Override
  public SorobanEndpointRaw setEncryptTo(PaymentCode encryptTo) {
    return (SorobanEndpointRaw) super.setEncryptTo(encryptTo);
  }

  @Override
  public SorobanEndpointRaw setDecryptFrom(PaymentCode decryptFrom) {
    return (SorobanEndpointRaw) super.setDecryptFrom(decryptFrom);
  }
}
