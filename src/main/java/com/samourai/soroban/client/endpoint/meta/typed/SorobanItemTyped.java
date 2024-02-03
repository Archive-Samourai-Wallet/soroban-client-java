package com.samourai.soroban.client.endpoint.meta.typed;

import com.samourai.soroban.client.endpoint.meta.SorobanItem;
import com.samourai.soroban.client.endpoint.meta.SorobanMetadata;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaType;
import com.samourai.soroban.client.exception.SorobanErrorMessageException;
import com.samourai.soroban.client.exception.UnexpectedSorobanPayloadTypedException;
import com.samourai.soroban.protocol.SorobanErrorMessage;
import com.samourai.wallet.util.JSONUtils;
import io.reactivex.functions.Consumer;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanItemTyped extends SorobanItem {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final JSONUtils jsonUtil = JSONUtils.getInstance();
  private Object payloadObject;

  public SorobanItemTyped(
      String payload, SorobanMetadata metadata, String rawEntry, SorobanEndpointTyped endpoint) {
    super(payload, metadata, rawEntry, endpoint);
  }

  public SorobanItemTyped(SorobanItem sorobanItem) {
    super(sorobanItem);
  }

  public String getType() {
    return SorobanWrapperMetaType.getType(getMetadata());
  }

  public boolean isTyped(Class type) {
    return getType().equals(type.getName());
  }

  protected Object read() throws Exception {
    if (payloadObject == null) {
      Class typeClass = Class.forName(getType());
      payloadObject = jsonUtil.getObjectMapper().readValue(getPayload(), typeClass);
    }
    return payloadObject;
  }

  public <P> P readOn(Class<P> type) throws Exception {
    return readOn(type, null);
  }

  public <P> P readOn(Class<P> type, Consumer<P> consumerOrNull) throws Exception {
    if (isTyped(type)) {
      // type matches
      P payloadTyped = (P) read();
      if (consumerOrNull != null) {
        consumerOrNull.accept(payloadTyped);
      }
      return payloadTyped;
    }
    return null;
  }

  public <R> R read(Class<R> type) throws Exception {
    R value = readOn(type);
    if (value != null) {
      return value;
    }

    // check for SorobanErrorMessage
    SorobanErrorMessage sorobanErrorMessage = readOn(SorobanErrorMessage.class);
    if (sorobanErrorMessage != null) {
      log.warn("SorobanError: " + sorobanErrorMessage);
      throw new SorobanErrorMessageException(sorobanErrorMessage);
    }

    log.warn(
        "read("
            + type.getName()
            + ") failed: UnexpectedSorobanPayloadTypedException (actual="
            + getType()
            + ")");

    // unexpected type
    throw new UnexpectedSorobanPayloadTypedException(this, Arrays.asList(type.getName()));
  }

  public SorobanEndpointTyped getEndpointReply() {
    return ((SorobanEndpointTyped) getEndpoint()).getEndpointReply(this);
  }
}
