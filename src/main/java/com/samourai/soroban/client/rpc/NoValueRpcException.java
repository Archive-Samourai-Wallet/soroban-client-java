package com.samourai.soroban.client.rpc;

public class NoValueRpcException extends Exception {
  public NoValueRpcException() {
    super("No value");
  }
}
