package com.samourai.soroban.client;

import com.samourai.soroban.client.dialog.SorobanErrorMessage;
import com.samourai.soroban.client.exception.SorobanErrorMessageException;
import com.samourai.soroban.client.exception.UnexpectedTypePayloadSorobanException;
import com.samourai.wallet.util.JSONUtils;
import io.reactivex.functions.Consumer;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UntypedPayload {
  private static final Logger log = LoggerFactory.getLogger(UntypedPayload.class);
  private static final JSONUtils jsonUtils = JSONUtils.getInstance();
  private String payload;
  private String _typePayload; // set by _parse()
  private long _timePayload; // set by _parse()

  public UntypedPayload(String payload) {
    this.payload = payload;
  }

  public <T extends SorobanPayload> T readOn(Class<T> type) throws Exception {
    return readOn(type, null);
  }

  public <T extends SorobanPayload> T readOn(Class<T> type, Consumer<T> consumerOrNull)
      throws Exception {
    if (isTypePayload(type)) {
      // type matches
      T sorobanPayload = jsonUtils.getObjectMapper().readValue(payload, type);
      if (consumerOrNull != null) {
        consumerOrNull.accept(sorobanPayload);
      }
      return sorobanPayload;
    }
    return null;
  }

  public <T extends SorobanPayload> T read(Class<T> type) throws Exception {
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

  private void _parse() {
    if (_typePayload == null) {
      try {
        JSONObject jsonObject = new JSONObject(payload);
        _typePayload = jsonObject.getString("typePayload");
        _timePayload = jsonObject.getLong("timePayload");
      } catch (Exception e) {
        log.error("Could not parse payload: " + payload, e);
      }
    }
  }

  public String getTypePayload() {
    _parse();
    return _typePayload;
  }

  public long getTimePayload() {
    _parse();
    return _timePayload;
  }

  public String getPayload() {
    return payload;
  }
}
