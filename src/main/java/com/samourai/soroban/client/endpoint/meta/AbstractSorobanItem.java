package com.samourai.soroban.client.endpoint.meta;

import com.samourai.soroban.client.endpoint.SorobanEndpoint;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;

public abstract class AbstractSorobanItem<M, E extends SorobanEndpoint> {
  private String payload;
  private M metadata;
  private String rawEntry;
  private E endpoint;

  public AbstractSorobanItem(String payload, M metadata, String rawEntry, E endpoint) {
    this.payload = payload;
    this.metadata = metadata;
    this.rawEntry = rawEntry;
    this.endpoint = endpoint;
  }

  public AbstractSorobanItem(AbstractSorobanItem<M, E> sorobanItem) {
    this(
        sorobanItem.getPayload(),
        sorobanItem.getMetadata(),
        sorobanItem.getRawEntry(),
        sorobanItem.getEndpoint());
  }

  public String getPayload() {
    return payload;
  }

  public M getMetadata() {
    return metadata;
  }

  public String getRawEntry() {
    return rawEntry;
  }

  public E getEndpoint() {
    return endpoint;
  }

  public E getEndpointReply(Bip47Encrypter encrypter) {
    return (E) getEndpoint().getEndpointReply(this, encrypter);
  }

  @Override
  public String toString() {
    return "{"
        + "payload='"
        + payload
        + '\''
        + ", metadata="
        + metadata
        + ", endpoint="
        + endpoint.toString()
        + "}";
  }
}
