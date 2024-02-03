package com.samourai.soroban.client.endpoint.meta;

import com.samourai.soroban.client.endpoint.AbstractSorobanEndpoint;
import com.samourai.soroban.client.endpoint.SorobanApp;
import com.samourai.soroban.client.endpoint.meta.wrapper.SorobanWrapperMeta;
import com.samourai.soroban.client.endpoint.wrapper.SorobanWrapper;
import com.samourai.soroban.client.endpoint.wrapper.SorobanWrapperString;
import com.samourai.soroban.client.rpc.RpcMode;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.util.Pair;
import com.samourai.wallet.util.Util;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSorobanEndpointMeta<I extends SorobanItem, L extends List<I>, S>
    extends AbstractSorobanEndpoint<I, L, S, SorobanMetadata> {
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
  protected String getRawEntry(SorobanItem entry) {
    return entry.getRawEntry();
  }

  @Override
  protected String entryToRaw(Pair<String, SorobanMetadata> entry) throws Exception {
    return new SorobanEntryMeta(entry.getLeft(), entry.getRight()).toPayload();
  }

  @Override
  protected Pair<String, SorobanMetadata> rawToEntry(String rawEntry) throws Exception {
    JSONObject jsonObject = new JSONObject(rawEntry);
    SorobanEntryMeta entryMeta = new SorobanEntryMeta(jsonObject);
    return Pair.of(entryMeta.getPayload(), entryMeta.getMetadata());
  }

  @Override
  protected Pair<String, SorobanMetadata> applyWrappersOnSend(
      Bip47Encrypter encrypter, Pair<String, SorobanMetadata> entry, Object initialPayload)
      throws Exception {
    // apply meta wrappers
    for (SorobanWrapperMeta wrapperMeta : wrappersMeta) {
      entry = wrapperMeta.onSend(encrypter, entry, initialPayload);
    }

    // apply string wrappers
    return super.applyWrappersOnSend(encrypter, entry, initialPayload);
  }

  @Override
  protected Pair<String, SorobanMetadata> applyWrappersOnReceive(
      Bip47Encrypter encrypter, Pair<String, SorobanMetadata> entry) throws Exception {
    // apply string wrappers
    entry = super.applyWrappersOnReceive(encrypter, entry);

    // apply meta wrappers
    try {
      for (SorobanWrapperMeta wrapperMeta : wrappersMeta) {
        entry = wrapperMeta.onReceive(encrypter, entry);
      }
    } catch (Exception e) {
      log.warn(" <- " + getDir() + ": " + entry + ": INVALID: " + e.getMessage());
      throw e;
    }
    return entry;
  }

  @Override
  public String computeUniqueId(I entry) {
    String uniqueId = entry.getPayload();
    PaymentCode sender = entry.getMetaSender();
    if (sender != null) {
      uniqueId += sender.toString();
    }
    return Util.sha256Hex(uniqueId);
  }
}
