package com.samourai.soroban.client.dialog;

import com.samourai.soroban.client.endpoint.AbstractSorobanEndpoint;
import com.samourai.soroban.client.endpoint.SorobanEndpoint;
import com.samourai.soroban.client.endpoint.meta.SorobanFilter;
import com.samourai.soroban.client.endpoint.wrapper.SorobanWrapperString;
import com.samourai.soroban.client.rpc.RpcMode;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.sorobanClient.SorobanPayloadable;
import com.samourai.wallet.util.Pair;

public class RpcDialogEndpoint
    extends AbstractSorobanEndpoint<
        RpcDialogItem, SorobanPayloadable, Void, SorobanFilter<RpcDialogItem>> {
  public RpcDialogEndpoint(String dir) {
    super(dir, RpcMode.FAST, new SorobanWrapperString[] {});
    setAutoRemove(true); // preserve existing behavior
  }

  @Override
  protected SorobanEndpoint newEndpointReply(RpcDialogItem request, Bip47Encrypter encrypter) {
    return null;
  }

  @Override
  protected Pair<String, Void> newEntry(SorobanPayloadable payload) throws Exception {
    return Pair.of(payload.toPayload(), null);
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
  protected RpcDialogItem newEntry(Pair<String, Void> entry, String rawEntry) {
    return new RpcDialogItem(entry.getLeft(), rawEntry, this);
  }

  @Override
  protected String getRawEntry(RpcDialogItem entry) {
    return entry.getRawEntry();
  }

  @Override
  protected SorobanFilter<RpcDialogItem> newFilterBuilder() {
    return null; // TODO
  }

  @Override
  public RpcDialogEndpoint setDecryptFrom(PaymentCode decryptFrom) {
    return (RpcDialogEndpoint) super.setDecryptFrom(decryptFrom);
  }

  @Override
  public RpcDialogEndpoint setEncryptTo(PaymentCode encryptTo) {
    return (RpcDialogEndpoint) super.setEncryptTo(encryptTo);
  }
}
