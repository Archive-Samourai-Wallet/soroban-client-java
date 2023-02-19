package com.samourai.soroban.client.rpc;

import com.samourai.http.client.IHttpClient;
import com.samourai.soroban.client.SorobanServer;
import com.samourai.wallet.util.AsyncUtil;
import io.reactivex.Single;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcClient {
  private Logger log;
  private static final int WAIT_RETRY_DELAY_MS = 1000;
  private static final String ENDPOINT_RPC = "/rpc";

  private final IHttpClient httpClient;
  private final String url;
  private boolean started;

  public RpcClient(String info, IHttpClient httpClient, NetworkParameters params, boolean onion) {
    this(info, httpClient, SorobanServer.get(params).getServerUrl(onion) + ENDPOINT_RPC);
  }

  protected RpcClient(String info, IHttpClient httpClient, String url) {
    this.log = LoggerFactory.getLogger(RpcClient.class.getName() + info);
    this.httpClient = httpClient;
    this.url = url;
    this.started = true;
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

  private Single<Map<String, Object>> call(String method, HashMap<String, Object> params)
      throws IOException {
    if (!started) {
      throw new IOException("RpcClient stopped");
    }

    Map<String, String> headers = new HashMap<String, String>();
    headers.put("content-type", "application/json");
    headers.put("User-Agent", "HotJava/1.1.2 FCS");

    HashMap<String, Object> body = new HashMap<String, Object>();
    body.put("method", method);
    body.put("jsonrpc", "2.0");
    body.put("id", 1);
    body.put("params", Arrays.asList(params));

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
                      throw new IOException("invalid status: " + status);
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
    HashMap<String, Object> params = new HashMap<String, Object>();
    params.put("Name", name);
    params.put("Entries", new String[0]);

    return call("directory.List", params)
        .map(
            rpc -> {
              Map<?, ?> result = (Map<?, ?>) rpc.get("result");
              ArrayList<?> src = (ArrayList<?>) result.get("Entries");
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
    long timeStart = System.currentTimeMillis();
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
              long elapsedTime = System.currentTimeMillis() - timeStart;
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
    HashMap<String, Object> params = new HashMap<String, Object>();

    params.put("Name", name);
    params.put("Entry", entry);
    params.put("Mode", mode);

    return call("directory.Add", params);
  }

  public Single<Map<String, Object>> directoryRemove(String name, String entry) throws IOException {
    if (log.isDebugEnabled()) {
      log.debug("--" + shortDirectory(name) + ": " + entry);
    }
    HashMap<String, Object> params = new HashMap<String, Object>();
    params.put("Name", name);
    params.put("Entry", entry);

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
}
