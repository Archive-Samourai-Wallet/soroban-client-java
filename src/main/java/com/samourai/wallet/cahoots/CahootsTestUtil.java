package com.samourai.wallet.cahoots;

public class CahootsTestUtil {
  public static void cleanPayload(Cahoots payload) throws Exception {
    // TODO static values for test
    payload.strID = "testID";
    payload.ts = 123456;
    payload.psbt = null;
  }
}
