package com.samourai.soroban.client.wrapper;

import com.samourai.soroban.client.SorobanClient;
import com.samourai.soroban.client.SorobanPayload;

public interface SorobanWrapper {
  SorobanPayload onSend(SorobanClient sorobanClient, SorobanPayload sorobanPayload)
      throws Exception;

  SorobanPayload onReceive(SorobanClient sorobanClient, SorobanPayload sorobanPayload)
      throws Exception;
}
