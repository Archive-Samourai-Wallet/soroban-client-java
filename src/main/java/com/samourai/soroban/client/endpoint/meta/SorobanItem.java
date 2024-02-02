package com.samourai.soroban.client.endpoint.meta;

import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaNonce;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaSender;
import com.samourai.wallet.bip47.rpc.PaymentCode;

public class SorobanItem {
  private String entry;
  private SorobanMetadata metadata;
  private String rawEntry;
  private AbstractSorobanEndpointMeta endpoint;

  public SorobanItem(
      String entry,
      SorobanMetadata metadata,
      String rawEntry,
      AbstractSorobanEndpointMeta endpoint) {
    this.entry = entry;
    this.metadata = metadata;
    this.rawEntry = rawEntry;
    this.endpoint = endpoint;
  }

  public SorobanItem(SorobanItem sorobanItem) {
    this(
        sorobanItem.getEntry(),
        sorobanItem.getMetadata(),
        sorobanItem.getRawEntry(),
        sorobanItem.getEndpoint());
  }

  public String getEntry() {
    return entry;
  }

  public SorobanMetadata getMetadata() {
    return metadata;
  }

  public String getRawEntry() {
    return rawEntry;
  }

  public String getUniqueId() {
    return endpoint.computeUniqueId(this);
  }

  public AbstractSorobanEndpointMeta getEndpoint() {
    return endpoint;
  }

  public PaymentCode getMetaSender() {
    return SorobanWrapperMetaSender.getSender(metadata);
  }

  public Long getMetaNonce() {
    return SorobanWrapperMetaNonce.getNonce(metadata);
  }
}