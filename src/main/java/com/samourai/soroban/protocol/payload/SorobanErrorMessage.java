package com.samourai.soroban.protocol.payload;

import com.samourai.soroban.client.AbstractSorobanPayloadable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanErrorMessage extends AbstractSorobanPayloadable {
  private static final Logger log = LoggerFactory.getLogger(SorobanErrorMessage.class.getName());
  public int errorCode;
  public String message;

  public SorobanErrorMessage() {}

  public SorobanErrorMessage(int errorCode, String message) {
    this.errorCode = errorCode;
    this.message = message;
  }

  @Override
  public String toString() {
    return "{" + "errorCode=" + errorCode + ", message='" + message + '\'' + '}';
  }
}
