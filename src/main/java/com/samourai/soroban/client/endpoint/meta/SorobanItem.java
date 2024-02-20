package com.samourai.soroban.client.endpoint.meta;

import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaNonce;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaSender;
import com.samourai.wallet.bip47.rpc.PaymentCode;

public class SorobanItem extends AbstractSorobanItem<SorobanMetadata, AbstractSorobanEndpointMeta> {

  public SorobanItem(
      String payload,
      SorobanMetadata metadata,
      String rawEntry,
      AbstractSorobanEndpointMeta endpoint) {
    super(payload, metadata, rawEntry, endpoint);
  }

  public SorobanItem(SorobanItem sorobanItem) {
    this(
        sorobanItem.getPayload(),
        sorobanItem.getMetadata(),
        sorobanItem.getRawEntry(),
        sorobanItem.getEndpoint());
  }

  public String getUniqueId() {
    return getEndpoint().computeUniqueId(this);
  }

  public PaymentCode getMetaSender() {
    return SorobanWrapperMetaSender.getSender(getMetadata());
  }

  public Long getMetaNonce() {
    return SorobanWrapperMetaNonce.getNonce(getMetadata());
  }
}
