package com.samourai.soroban.client.endpoint.meta;

import com.samourai.soroban.client.endpoint.AbstractSorobanEndpoint;
import com.samourai.soroban.client.endpoint.SorobanApp;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMeta;
import com.samourai.soroban.client.endpoint.wrapper.SorobanWrapper;
import com.samourai.soroban.client.endpoint.wrapper.SorobanWrapperString;
import com.samourai.soroban.client.rpc.RpcMode;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.util.Util;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSorobanEndpointMeta<I extends SorobanItem, L>
    extends AbstractSorobanEndpoint<I, L, SorobanEntryMeta> {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private SorobanWrapperMeta[] wrappersMeta;

  public AbstractSorobanEndpointMeta(
      SorobanApp app, String path, RpcMode rpcMode, SorobanWrapper[] wrappers) {
    super(app, path, rpcMode, findWrappersString(wrappers));
    this.wrappersMeta = findWrappersMeta(wrappers);
  }

  private static SorobanWrapperMeta[] findWrappersMeta(SorobanWrapper[] wrappers) {
    SorobanWrapperMeta[] wrappersMeta =
        Arrays.stream(wrappers)
            .filter(wrapper -> SorobanWrapperMeta.class.isAssignableFrom(wrapper.getClass()))
            .toArray(n -> new SorobanWrapperMeta[n]);
    if (log.isDebugEnabled()) {
      log.debug("wrappersMeta: " + Arrays.toString(wrappersMeta));
    }
    return wrappersMeta;
  }

  private static SorobanWrapperString[] findWrappersString(SorobanWrapper[] wrappers) {
    SorobanWrapperString[] wrappersString =
        Arrays.stream(wrappers)
            .filter(wrapper -> SorobanWrapperString.class.isAssignableFrom(wrapper.getClass()))
            .toArray(n -> new SorobanWrapperString[n]);
    if (log.isDebugEnabled()) {
      log.debug("wrappersString: " + Arrays.toString(wrappersString));
    }
    return wrappersString;
  }

  @Override
  protected SorobanEntryMeta newEntry(String payload) throws Exception {
    return new SorobanEntryMeta(payload);
  }

  @Override
  protected String getRawEntry(SorobanItem entry) {
    return entry.getRawEntry();
  }

  @Override
  protected String toPayload(SorobanEntryMeta entry) throws Exception {
    return entry.toPayload();
  }

  @Override
  protected String applyWrappersForSend(
      Bip47Encrypter encrypter, SorobanEntryMeta entryObject, Object initialPayload)
      throws Exception {
    // apply meta wrappers
    for (SorobanWrapperMeta wrapperMeta : wrappersMeta) {
      wrapperMeta.onSend(encrypter, entryObject, initialPayload);
    }
    return super.applyWrappersForSend(encrypter, entryObject, initialPayload);
  }

  @Override
  protected final SorobanEntryMeta readEntry(Bip47Encrypter encrypter, String entry)
      throws Exception {
    try {
      JSONObject jsonObject = new JSONObject(entry);
      SorobanEntryMeta entryMeta = new SorobanEntryMeta(jsonObject);

      // apply wrappers
      for (SorobanWrapperMeta wrapperMeta : wrappersMeta) {
        entryMeta = wrapperMeta.onReceive(encrypter, entryMeta);
      }
      if (log.isDebugEnabled()) {
        log.debug(" <- " + getDir() + ": " + entryMeta.toPayload());
      }
      return entryMeta;
    } catch (Exception e) {
      log.warn(" <- " + getDir() + ": " + entry + ": INVALID: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public String computeUniqueId(I entry) {
    String uniqueId = entry.getEntry();
    PaymentCode sender = entry.getMetaSender();
    if (sender != null) {
      uniqueId += sender.toString();
    }
    return Util.sha256Hex(uniqueId);
  }
}
