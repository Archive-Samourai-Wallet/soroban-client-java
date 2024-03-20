package com.samourai.soroban.client.endpoint.meta;

import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaNonce;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaSender;
import com.samourai.wallet.bip47.rpc.PaymentCode;

public class SorobanItem extends AbstractSorobanItem<SorobanMetadata, AbstractSorobanEndpointMeta> {
  private String uniqueId;
  private Long nonce;
  private PaymentCode sender;

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
    if (uniqueId == null) {
      uniqueId = getEndpoint().computeUniqueId(this);
    }
    return uniqueId;
  }

  public PaymentCode getMetaSender() {
    if (sender == null) {
      sender = SorobanWrapperMetaSender.getSender(getMetadata());
    }
    return sender;
  }

  public Long getMetaNonce() {
    if (nonce == null) {
      nonce = SorobanWrapperMetaNonce.getNonce(getMetadata());
    }
    return nonce;
  }
}
