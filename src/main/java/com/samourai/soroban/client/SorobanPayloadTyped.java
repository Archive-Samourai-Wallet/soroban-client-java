package com.samourai.soroban.client;

import com.samourai.soroban.client.dialog.SorobanErrorMessage;
import com.samourai.soroban.client.endpoint.SorobanPayloadImpl;
import com.samourai.soroban.client.exception.SorobanErrorMessageException;
import com.samourai.soroban.client.exception.SorobanException;
import com.samourai.soroban.client.exception.UnexpectedTypePayloadSorobanException;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import io.reactivex.functions.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanPayloadTyped extends SorobanPayloadImpl {
  private static final Logger log = LoggerFactory.getLogger(SorobanPayloadTyped.class);

  private String typePayload;
  private long timePayload;

  public SorobanPayloadTyped(String sender, String payload, String typePayload, long timePayload) {
    super(sender, payload); // TODO
    this.typePayload = typePayload;
    this.timePayload = timePayload;
  }

  public SorobanPayloadTyped(String sender, String payload, String typePayload) {
    this(sender, payload, typePayload, System.currentTimeMillis());
  }

  public SorobanPayloadTyped(SorobanPayloadable sorobanPayloadable, PaymentCode sender)
      throws Exception {
    this(
        sender.toString(),
        sorobanPayloadable.toPayload(),
        sorobanPayloadable.getClass().getName(),
        System.currentTimeMillis());
  }

  public static SorobanPayloadTyped parse(String json) throws SorobanException {
    return null; // TODO
    /*try {
      JSONObject jsonObject = new JSONObject(json);
      String payload = jsonObject.getString("payload");
      String typePayload = jsonObject.getString("typePayload");
      long timePayload = jsonObject.getLong("timePayload");
      return new SorobanPayloadTyped(payload, typePayload, timePayload);
    } catch (Exception e) {
      throw new Exception("Could not parse payload: " + json, e);
    }*/
  }

  public <T extends SorobanPayloadable> T readOn(Class<T> type) throws Exception {
    return readOn(type, null);
  }

  public <T extends SorobanPayloadable> T readOn(Class<T> type, Consumer<T> consumerOrNull)
      throws Exception {
    if (isTypePayload(type)) {
      // type matches
      T sorobanPayload = null; // TODO jsonUtils.getObjectMapper().readValue(getPayload(), type);
      if (consumerOrNull != null) {
        consumerOrNull.accept(sorobanPayload);
      }
      return sorobanPayload;
    }
    return null;
  }

  public <T extends SorobanPayloadable> T read(Class<T> type) throws Exception {
    T value = readOn(type);
    if (value != null) {
      return value;
    }

    // check for SorobanErrorMessage
    SorobanErrorMessage sorobanErrorMessage = readOn(SorobanErrorMessage.class);
    if (sorobanErrorMessage != null) {
      throw new SorobanErrorMessageException(sorobanErrorMessage);
    }

    // unexpected type
    throw new UnexpectedTypePayloadSorobanException(this, type);
  }

  public boolean isTypePayload(Class type) {
    return type.getName().equals(getTypePayload());
  }

  public String getTypePayload() {
    return typePayload;
  }

  public long getTimePayload() {
    return timePayload;
  }
}
