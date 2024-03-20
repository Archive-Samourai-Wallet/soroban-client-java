package com.samourai.soroban.client.endpoint.meta.typed;

import com.samourai.soroban.client.endpoint.meta.SorobanItem;
import com.samourai.soroban.client.endpoint.meta.SorobanMetadata;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMetaType;
import com.samourai.soroban.client.exception.SorobanErrorMessageException;
import com.samourai.soroban.client.exception.UnexpectedSorobanItemTypeException;
import com.samourai.soroban.protocol.payload.SorobanErrorMessage;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import com.samourai.wallet.util.JSONUtils;
import io.reactivex.functions.Consumer;
import java.lang.invoke.MethodHandles;
import java.util.Optional;
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

  public <P> Optional<P> readOn(Class<P> type) throws Exception {
    return readOn(type, null);
  }

  public <P> Optional<P> readOn(Class<P> type, Consumer<P> consumerOrNull) throws Exception {
    if (isTyped(type)) {
      // type matches
      P payloadTyped = (P) read();
      if (consumerOrNull != null) {
        consumerOrNull.accept(payloadTyped);
      }
      return Optional.of(payloadTyped);
    }
    return Optional.empty();
  }

  public <R> R read(Class<R> type) throws Exception {
    // read from type when it matches
    Optional<R> opt = readOn(type);
    if (opt.isPresent()) {
      return opt.get();
    }

    // check for error
    throwOnSorobanErrorMessage();

    // unexpected type
    log.warn(
        "read("
            + type.getName()
            + ") failed: UnexpectedSorobanPayloadTypedException (actual="
            + getType()
            + ")");
    throw new UnexpectedSorobanItemTypeException(this, new Class[] {type});
  }

  public Optional<SorobanErrorMessage> readOnError() throws Exception {
    Optional<SorobanErrorMessage> optError = readOn(SorobanErrorMessage.class);
    if (optError.isPresent()) {
      if (log.isDebugEnabled()) {
        log.warn("SorobanError: " + optError.get());
      }
      return Optional.of(optError.get());
    }
    return Optional.empty();
  }

  public void throwOnSorobanErrorMessage() throws Exception {
    Optional<SorobanErrorMessage> errorMessage = readOnError();
    if (errorMessage.isPresent()) {
      throw new SorobanErrorMessageException(errorMessage.get());
    }
  }

  public SorobanEndpointTyped getEndpointReply(Bip47Encrypter encrypter) {
    return (SorobanEndpointTyped) super.getEndpointReply(encrypter);
  }
}
