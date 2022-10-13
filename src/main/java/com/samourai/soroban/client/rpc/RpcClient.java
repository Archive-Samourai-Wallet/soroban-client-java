package com.samourai.soroban.client.rpc;

import com.samourai.http.client.IHttpClient;
import com.samourai.wallet.util.AsyncUtil;
import io.reactivex.Single;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcClient {
  private static final Logger log = LoggerFactory.getLogger(RpcClient.class);

  private static final int WAIT_DELAY_MS = 500;

  private final IHttpClient httpClient;
  private final String url;
  private boolean started;

  public RpcClient(IHttpClient httpClient, String url) {
    this.httpClient = httpClient;
    this.url = url;
    this.started = true;
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
                throw new Exception("No value");
              }
              String value = values[values.length - 1];
              if (value.isEmpty()) {
                throw new Exception("No value");
              }
              return value;
            });
  }

  public Single<String> directoryValueWait(final String name, final long timeoutMs) {
    return Single.fromCallable(
        () -> {
          long timeStart = System.currentTimeMillis();
          long elapsedTime;
          while (started) {
            elapsedTime = System.currentTimeMillis() - timeStart;

            try {
              String value = AsyncUtil.getInstance().blockingGet(directoryValue(name));
              return value;
            } catch (Exception e) {
              // null value
            }

            // always check values at least once, or more when timeout not expired
            if (elapsedTime >= timeoutMs) {
              throw new TimeoutException(
                  String.format("Waited " + Math.round(elapsedTime / 1000) + "s, aborting"));
            }
            try {
              Thread.sleep(WAIT_DELAY_MS);
            } catch (InterruptedException e) {
            }
          }
          throw new TimeoutException("exit"); // simulate TimeoutException on exit()
        });
  }

  public Single directoryAdd(String name, String entry, String mode) throws IOException {
    HashMap<String, Object> params = new HashMap<String, Object>();

    params.put("Name", name);
    params.put("Entry", entry);
    params.put("Mode", mode);

    return call("directory.Add", params);
  }

  public Single<Map<String, Object>> directoryRemove(String name, String entry) throws IOException {
    HashMap<String, Object> params = new HashMap<String, Object>();
    params.put("Name", name);
    params.put("Entry", entry);

    return call("directory.Remove", params);
  }

  public Single<String> waitAndRemove(final String name, final long timeoutMs) throws Exception {
    return directoryValueWait(name, timeoutMs)
        .map(
            value -> {
              directoryRemove(name, value).subscribe();
              return value;
            });
  }
}
