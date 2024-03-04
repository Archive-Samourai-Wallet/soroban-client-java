package com.samourai.soroban.client.endpoint.meta;

import com.samourai.soroban.client.endpoint.wrapper.SorobanWrapper;
import com.samourai.soroban.client.rpc.RpcMode;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.util.Pair;

/**
 * This endpoint sends & receives String payloads, with full Soroban features support. Payloads are
 * wrapped with metadatas.
 */
public class SorobanEndpointMetaString extends AbstractSorobanEndpointMeta<SorobanItem, String> {

  public SorobanEndpointMetaString(String dir, RpcMode rpcMode, SorobanWrapper[] wrappersAll) {
    super(dir, rpcMode, wrappersAll);
  }

  @Override
  protected Pair<String, SorobanMetadata> newEntry(String payload) throws Exception {
    return Pair.of(payload, new SorobanMetadataImpl());
  }

  @Override
  protected SorobanItem newEntry(Pair<String, SorobanMetadata> entry, String rawEntry) {
    return new SorobanItem(entry.getLeft(), entry.getRight(), rawEntry, this);
  }

  @Override
  public SorobanEndpointMetaString newEndpointReply(SorobanItem request, Bip47Encrypter encrypter) {
    SorobanEndpointMetaString endpoint =
        new SorobanEndpointMetaString(
            getDirReply(request), getReplyRpcMode(), new SorobanWrapper[] {});
    return endpoint;
  }

  @Override
  public SorobanEndpointMetaString setEncryptTo(PaymentCode encryptTo) {
    return (SorobanEndpointMetaString) super.setEncryptTo(encryptTo);
  }

  @Override
  public SorobanEndpointMetaString setEncryptToWithSender(PaymentCode encryptTo) {
    return (SorobanEndpointMetaString) super.setEncryptToWithSender(encryptTo);
  }

  @Override
  public SorobanEndpointMetaString setDecryptFrom(PaymentCode decryptFrom) {
    return (SorobanEndpointMetaString) super.setDecryptFrom(decryptFrom);
  }

  @Override
  public SorobanEndpointMetaString setDecryptFromSender() {
    return (SorobanEndpointMetaString) super.setDecryptFromSender();
  }
}
