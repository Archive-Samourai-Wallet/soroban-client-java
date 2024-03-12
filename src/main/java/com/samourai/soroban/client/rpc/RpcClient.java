package com.samourai.soroban.client.rpc;

import com.samourai.wallet.bipFormat.BIP_FORMAT;
import com.samourai.wallet.httpClient.IHttpClient;
import com.samourai.wallet.util.AsyncUtil;
import com.samourai.wallet.util.MessageSignUtilGeneric;
import com.samourai.wallet.util.ShutdownException;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcClient {
  private static final Logger log = LoggerFactory.getLogger(RpcClient.class.getName());
  public static final String ENDPOINT_RPC = "/rpc";

  private final IHttpClient httpClient;
  private final String url;
  private NetworkParameters params;
  private boolean started;
  private ECKey authenticationKey;

  protected RpcClient(IHttpClient httpClient, String url, NetworkParameters params) {
    this.httpClient = httpClient;
    this.url = url;
    this.params = params;
    this.started = true;
    this.authenticationKey = null;
  }

  public void setAuthenticationKey(ECKey authenticationKey) {
    this.authenticationKey = authenticationKey;
  }

  public String getUrl() {
    return url;
  }

  public IHttpClient getHttpClient() {
    return httpClient;
  }

  public void exit() {
    this.started = false;
  }

  private Single<Map<String, Object>> call(String method, Map<String, Object> params) {
    if (!started) {
      return Single.error(new ShutdownException("RpcClient stopped"));
    }

    Map<String, String> headers = new HashMap<>();
    headers.put("content-type", "application/json");
    headers.put("User-Agent", "HotJava/1.1.2 FCS");

    Map<String, Object> body = new HashMap<>();
    body.put("method", method);
    body.put("jsonrpc", "2.0");
    body.put("id", 1);
    body.put("params", Arrays.asList(params));

    if (log.isTraceEnabled()) {
      log.trace("call: -> " + method + " " + url + " " + params);
    }

    Single<Map<String, Object>> result =
        httpClient
            .postJson(url, Map.class, headers, body)
            .map(
                rpcOpt -> {
                  Map<String, Object> rpc = rpcOpt.get();

                  if (log.isTraceEnabled()) {
                    log.trace("call: <- " + rpc);
                  }

                  // check error
                  String error = (String) rpc.get("error");
                  if (error != null) {
                    throw new IOException("Error: " + error);
                  }

                  // check status if any
                  Map<String, String> result1 = (Map<String, String>) rpc.get("result");
                  if (result1 != null) {
                    String status = result1.get("Status");
                    if (status != null && !status.equals("success")) {
                      throw new IOException("RPC call failed: status=" + status);
                    }
                  }
                  return rpc;
                });
    return result;
  }

  public Single<String[]> directoryValues(String name) {
    Map<String, Object> params = computeParams(name);
    return call("directory.List", params)
        .map(
            rpc -> {
              Map<?, ?> result = (Map<?, ?>) rpc.get("result");
              ArrayList<?> src = (ArrayList<?>) result.get("Entries");
              if (src == null) {
                throw new PermissionDeniedRpcException();
              }
              String[] dest = new String[src.size()];
              System.arraycopy(src.toArray(), 0, dest, 0, src.size());
              if (log.isDebugEnabled()) {
                log.debug("<= list(" + src.size() + ") sorobanDir=" + name + ", sorobanUrl=" + url);
              }
              return dest;
            });
  }

  public Single<String> directoryValue(String name) throws IOException {
    return directoryValues(name)
        .map(
            values -> {
              if (values == null || values.length == 0) {
                throw new NoValueRpcException();
              }
              String value = values[values.length - 1];
              if (value.isEmpty()) {
                throw new NoValueRpcException();
              }
              return value;
            });
  }

  public Completable directoryAdd(String name, String entry, RpcMode rpcMode) {
    if (log.isTraceEnabled()) {
      log.trace("=> add sorobanDir=" + name + ", sorobanUrl=" + url + " => " + entry);
    } else if (log.isDebugEnabled()) {
      log.debug("=> add sorobanDir=" + name + ", sorobanUrl=" + url);
    }
    Map<String, Object> params = computeParams(name, entry);
    params.put("Mode", rpcMode.getValue());
    return Completable.fromSingle(call("directory.Add", params));
  }

  public Completable directoryRemove(String name, String entry) {
    if (log.isDebugEnabled()) {
      log.debug("=> remove sorobanDir=" + name + ": " + entry + ", sorobanUrl=" + url);
    }
    Map<String, Object> params = computeParams(name, entry);
    return Completable.fromSingle(call("directory.Remove", params));
  }

  public Completable directoryRemoveAll(String name) {
    return Completable.fromSingle(
        directoryValues(name)
            .map(
                entries -> {
                  Arrays.stream(entries)
                      .forEach(
                          entry -> {
                            try {
                              AsyncUtil.getInstance().blockingAwait(directoryRemove(name, entry));
                            } catch (Exception e) {
                            }
                          });
                  return entries;
                }));
  }

  protected Map<String, Object> computeParams(String name, String entryOrNull) {
    Map<String, Object> params = new HashMap<>();
    params.put("Name", name);
    if (entryOrNull != null) {
      params.put("Entry", entryOrNull);
    }
    if (authenticationKey != null) {
      // authenticate
      String signatureAddress = BIP_FORMAT.LEGACY.getToAddress(authenticationKey, this.params);
      long timestamp = Instant.now().toEpochMilli() * 1000000;
      String signedMessage =
          name + "." + timestamp + (entryOrNull != null ? "." + entryOrNull : "");
      if (log.isTraceEnabled()) {
        log.trace("signatureAddress=" + signatureAddress + ", signedMessage=" + signedMessage);
      }
      params.put("Algorithm", "testnet3");
      params.put("PublicKey", signatureAddress);
      params.put("Timestamp", timestamp);
      String signature =
          MessageSignUtilGeneric.getInstance().signMessage(authenticationKey, signedMessage);
      params.put("Signature", signature);
    }
    return params;
  }

  protected Map<String, Object> computeParams(String name) {
    return computeParams(name, null);
  }

  public NetworkParameters getParams() {
    return params;
  }
}
