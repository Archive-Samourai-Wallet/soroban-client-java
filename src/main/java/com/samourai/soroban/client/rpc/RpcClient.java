package com.samourai.soroban.client.rpc;

import com.samourai.http.client.IHttpClient;
import com.samourai.soroban.client.RpcWallet;
import com.samourai.soroban.client.SorobanMessage;
import com.samourai.soroban.client.SorobanServerDex;
import com.samourai.soroban.client.dialog.Encrypter;
import com.samourai.soroban.client.dialog.RpcDialog;
import com.samourai.wallet.bipFormat.BIP_FORMAT;
import com.samourai.wallet.crypto.CryptoUtil;
import com.samourai.wallet.util.AsyncUtil;
import com.samourai.wallet.util.MessageSignUtilGeneric;
import io.reactivex.Single;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.ArrayUtils;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcClient {
  private Logger log;
  private static final int WAIT_RETRY_DELAY_MS = 1000;
  private static final String ENDPOINT_RPC = "/rpc";

  private final IHttpClient httpClient;
  private final CryptoUtil cryptoUtil;
  private final String url;
  private boolean started;

  private ECKey authenticationKey; // null until setAuthentication()
  private NetworkParameters networkParams; // null until setAuthentication()

  public RpcClient(
      String info,
      IHttpClient httpClient,
      CryptoUtil cryptoUtil,
      NetworkParameters params,
      boolean onion) {
    this(
        info,
        httpClient,
        cryptoUtil,
        SorobanServerDex.get(params).getServerUrlRandom(onion) + ENDPOINT_RPC);
  }

  protected RpcClient(String info, IHttpClient httpClient, CryptoUtil cryptoUtil, String url) {
    this.log = LoggerFactory.getLogger(RpcClient.class.getName() + info);
    this.httpClient = httpClient;
    this.cryptoUtil = cryptoUtil;
    this.url = url;
    this.started = true;
    this.authenticationKey = null;
  }

  public RpcClientEncrypted createRpcClientEncrypted(RpcWallet rpcWallet) {
    Encrypter encrypter = cryptoUtil.getEncrypter(rpcWallet);
    return new RpcClientEncrypted(this, encrypter, null);
  }

  public RpcDialog createRpcDialog(RpcWallet rpcWallet, String directory) throws Exception {
    Encrypter encrypter = cryptoUtil.getEncrypter(rpcWallet);
    return new RpcDialog(directory, this, encrypter);
  }

  public RpcDialog createRpcDialog(RpcWallet rpcWallet, SorobanMessage sorobanMessage)
      throws Exception {
    return createRpcDialog(rpcWallet, sorobanMessage.toPayload());
  }

  public static String shortDirectory(String directory) {
    return directory.substring(0, Math.min(directory.length(), 5));
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

  private Single<Map<String, Object>> call(String method, Map<String, Object> params)
      throws IOException {
    if (!started) {
      throw new IOException("RpcClient stopped");
    }

    Map<String, String> headers = new HashMap<>();
    headers.put("content-type", "application/json");
    headers.put("User-Agent", "HotJava/1.1.2 FCS");

    Map<String, Object> body = new HashMap<>();
    body.put("method", method);
    body.put("jsonrpc", "2.0");
    body.put("id", 1);
    body.put("params", Arrays.asList(params));

    if (log.isDebugEnabled()) {
      log.debug("call: body=" + ArrayUtils.toString(body));
    }

    Single<Map<String, Object>> result =
        httpClient
            .postJson(url, Map.class, headers, body)
            .map(
                rpcOpt -> {
                  Map<String, Object> rpc = rpcOpt.get();

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

  public Single<String[]> directoryValues(String name) throws IOException {
    if (log.isDebugEnabled()) {
      log.debug("get " + shortDirectory(name));
    }
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

  public Single<String> directoryValueWait(final String name, final long timeoutMs)
      throws IOException {
    if (log.isDebugEnabled()) {
      log.debug("wait " + shortDirectory(name) + "... ");
    }
    long timeStart = System.nanoTime();
    return directoryValue(name)
        .retry(
            error -> {
              try {
                // wait for retry delay
                AsyncUtil.getInstance()
                    .blockingGet(Single.timer(WAIT_RETRY_DELAY_MS, TimeUnit.MILLISECONDS));
              } catch (Exception e) {
                // ok
              }
              if (!started) {
                if (log.isDebugEnabled()) {
                  log.debug("exit");
                }
                return false; // exit
              }
              long elapsedTime = System.nanoTime() - timeStart;
              if (log.isTraceEnabled()) {
                log.trace(
                    "Waiting for "
                        + shortDirectory(name)
                        + "... "
                        + elapsedTime
                        + "/"
                        + timeoutMs
                        + "ms");
              }
              return true; // retry
            })
        .timeout(timeoutMs, TimeUnit.MILLISECONDS);
  }

  public Single directoryAdd(String name, String entry, String mode) throws IOException {
    if (log.isDebugEnabled()) {
      log.debug("add " + shortDirectory(name) + ": " + entry);
    }
    Map<String, Object> params = computeParams(name, entry);
    params.put("Mode", mode);

    return call("directory.Add", params);
  }

  public Single<Map<String, Object>> directoryRemove(String name, String entry) throws IOException {
    if (log.isDebugEnabled()) {
      log.debug("--" + shortDirectory(name) + ": " + entry);
    }
    Map<String, Object> params = computeParams(name, entry);
    return call("directory.Remove", params);
  }

  public Single<String> directoryValueWaitAndRemove(final String name, final long timeoutMs)
      throws Exception {
    return directoryValueWait(name, timeoutMs)
        .map(
            value -> {
              directoryRemove(name, value).subscribe();
              return value;
            });
  }

  protected Map<String, Object> computeParams(String name, String entryOrNull) {
      Map<String, Object> params = new HashMap<>();
      params.put("Name", name);
      appendParamsAuthentication(name, params, entryOrNull);
      if (entryOrNull != null) {
          params.put("Entry", entryOrNull);
      }
      return params;
  }

  protected Map<String, Object> computeParams(String name) {
    return computeParams(name, null);
  }

  protected void appendParamsAuthentication(String name, Map<String, Object> params, String entryOrNull) {
    if (authenticationKey != null) {
      String signatureAddress = BIP_FORMAT.LEGACY.getToAddress(authenticationKey, networkParams);
        long timestamp = Instant.now().toEpochMilli()*1000000;
      String signedMessage = name+"."+timestamp+(entryOrNull!=null?"."+entryOrNull:"");
      if (log.isDebugEnabled()) {
        log.debug("signatureAddress=" + signatureAddress+", signedMessage="+signedMessage);
      }
      params.put("Algorithm", "testnet3");
      params.put("PublicKey", signatureAddress);
      params.put("Timestamp", timestamp);
      String signature = MessageSignUtilGeneric.getInstance().signMessage(authenticationKey, signedMessage);
      params.put("Signature", signature);
    }
  }

  public void setAuthentication(ECKey signatureKey, NetworkParameters networkParams) {
    this.authenticationKey = signatureKey;
    this.networkParams = networkParams;
  }
}
