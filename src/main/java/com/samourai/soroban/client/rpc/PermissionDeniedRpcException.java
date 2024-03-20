package com.samourai.soroban.client.rpc;

public class PermissionDeniedRpcException extends Exception {
  public PermissionDeniedRpcException() {
    super("RPC permission denied");
  }
}
