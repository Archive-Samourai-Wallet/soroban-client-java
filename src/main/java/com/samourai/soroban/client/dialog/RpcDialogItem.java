package com.samourai.soroban.client.dialog;

import com.samourai.soroban.client.endpoint.meta.AbstractSorobanItem;
import com.samourai.soroban.client.meeting.SorobanMessageWithSender;

public class RpcDialogItem extends AbstractSorobanItem<Void, RpcDialogEndpoint> {

  public RpcDialogItem(String payload, String rawEntry, RpcDialogEndpoint endpoint) {
    super(payload, null, rawEntry, endpoint);
  }

  public RpcDialogItem(AbstractSorobanItem<Void, RpcDialogEndpoint> sorobanItem) {
    super(sorobanItem);
  }

  public void readSorobanMessageWithSender() throws Exception {
    SorobanMessageWithSender.parse(getPayload());
  }
}
