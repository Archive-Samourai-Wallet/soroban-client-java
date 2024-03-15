package com.samourai.soroban.client.rpc;

// see
// https://code.samourai.io/wallet/samourai-soroban/-/blob/master/internal/common/ttl.go?ref_type=heads
public enum RpcMode {
  NORMAL(
      "normal", 180000, // 3min
      90000, // 1min30
      15000), // 15s
  SHORT(
      "short", 60000, // 1min
      30000, // 30s
      5000), // 5s
  FAST(
      "fast", 20000, // 20s (Soroban real expiry: 15s, limit increased for Tor/mobile handling)
      7000, // 7s
      3000), // 3s
  ; // 2s

  private String value;

  // Soroban messages expiration
  // used for: default endpoint.waitReply() timeout, default sorobanController.expiration, default
  // loopTimeoutMs(x2)
  private int expirationMs;

  // used for: endpoint.loopSendUntil() sending frequency
  private int resendFrequencyWhenNoReplyMs;

  // used for: endpoint.waitAny(), default endpointController.loopDelay
  private int pollingFrequencyMs;

  RpcMode(
      String value, int expirationMs, int resendFrequencyWhenNoReplyMs, int pollingFrequencyMs) {
    this.value = value;
    this.expirationMs = expirationMs;
    this.resendFrequencyWhenNoReplyMs = resendFrequencyWhenNoReplyMs;
    this.pollingFrequencyMs = pollingFrequencyMs;
  }

  public String getValue() {
    return value;
  }

  public int getExpirationMs() {
    return expirationMs;
  }

  public int getResendFrequencyWhenNoReplyMs() {
    return resendFrequencyWhenNoReplyMs;
  }

  public int getPollingFrequencyMs() {
    return pollingFrequencyMs;
  }
}
