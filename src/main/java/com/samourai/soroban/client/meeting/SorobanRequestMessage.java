package com.samourai.soroban.client.meeting;

import com.samourai.soroban.client.AbstractSorobanMessage;
import com.samourai.wallet.cahoots.CahootsType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanRequestMessage extends AbstractSorobanMessage {
  private static final Logger log = LoggerFactory.getLogger(SorobanRequestMessage.class);

  private String senderPaymentCode;
  private String description;
  private CahootsType type;

  public SorobanRequestMessage() {
    this(null, null, null);
  }

  public SorobanRequestMessage(String senderPaymentCode, String description, CahootsType type) {
    super(true);
    this.senderPaymentCode = senderPaymentCode;
    this.description = description;
    this.type = type;
  }

  public static SorobanRequestMessage parse(String payload) throws Exception {
    return parse(payload, SorobanRequestMessage.class);
  }

  @Override
  public boolean isLastMessage() {
    return true;
  }

  public String getSenderPaymentCode() {
    return senderPaymentCode;
  }

  public void setSenderPaymentCode(String senderPaymentCode) {
    this.senderPaymentCode = senderPaymentCode;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public CahootsType getType() {
    return type;
  }

  public void setType(CahootsType type) {
    this.type = type;
  }

  @Override
  public String toString() {
    return "Type: " + type + "\nDescription: " + description;
  }
}
