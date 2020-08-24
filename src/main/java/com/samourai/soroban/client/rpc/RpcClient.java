package com.samourai.soroban.client.rpc;

import com.google.common.base.Charsets;
import com.samourai.http.client.IHttpClient;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcClient {
  private static final Logger log = LoggerFactory.getLogger(RpcClient.class);

  private static final int WAIT_DELAY_MS = 200;

  private IHttpClient httpclient;
  private String url;

  public RpcClient(IHttpClient httpClient, String url) {
    this.httpclient = httpClient;
    this.url = url;
  }

  private Map<String, Object> call(String method, HashMap<String, Object> params)
      throws IOException {

    Map<String, String> headers = new HashMap<String, String>();
    headers.put("content-type", "application/json");
    headers.put("User-Agent", "HotJava/1.1.2 FCS");

    HashMap<String, Object> body = new HashMap<String, Object>();
    body.put("method", method);
    body.put("jsonrpc", "2.0");
    body.put("id", 1);
    body.put("params", Arrays.asList(params));

    Map<String, Object> result =
        httpclient.postJson(url, Map.class, headers, body).blockingSingle().get();
    return result;
  }

  public String[] directoryList(String name) throws IOException {
    HashMap<String, Object> params = new HashMap<String, Object>();
    params.put("Name", name);
    params.put("Entries", new String[0]);

    Map<?, ?> rpc = call("directory.List", params);
    if (rpc.get("error") != null) {
      return new String[0];
    }
    Map<?, ?> result = (Map<?, ?>) rpc.get("result");

    ArrayList<?> src = (ArrayList<?>) result.get("Entries");
    String[] dest = new String[src.size()];
    System.arraycopy(src.toArray(), 0, dest, 0, src.size());
    return dest;
  }

  public void directoryAdd(String name, String entry, String mode) throws IOException {
    HashMap<String, Object> params = new HashMap<String, Object>();

    params.put("Name", name);
    params.put("Entry", entry);
    params.put("Mode", mode);

    Map<?, ?> rpc = call("directory.Add", params);
    if (rpc.get("error") != null) {
      throw new IOException("Error: " + rpc.get("error"));
    }

    Map<?, ?> result = (Map<?, ?>) rpc.get("result");
    String status = result.get("Status").toString();
    if (!status.equals("success")) {
      throw new IOException("invalid status: " + status);
    }
    if (log.isDebugEnabled()) {
      log.debug("=> " + name + " (" + mode + ")");
    }
  }

  public void directoryRemove(String name, String entry) throws IOException {
    HashMap<String, Object> params = new HashMap<String, Object>();
    params.put("Name", name);
    params.put("Entry", entry);

    Map<?, ?> rpc = call("directory.Remove", params);
    if (rpc.get("error") != null) {
      throw new IOException("Error: " + rpc.get("error"));
    }

    Map<?, ?> result = (Map<?, ?>) rpc.get("result");
    String status = result.get("Status").toString();
    if (!status.equals("success")) {
      throw new IOException("invalid status: " + status);
    }
  }

  public String waitAndRemove(String name, long timeoutMs) throws Exception {
    String[] values;

    long timeStart = System.currentTimeMillis();
    long elapsedTime;
    while (true) {
      elapsedTime = System.currentTimeMillis() - timeStart;

      values = directoryList(name);
      if (values.length > 0) {
        break;
      }

      // always check values at least once, or more when timeout not expired
      if (elapsedTime >= timeoutMs) {
        throw new TimeoutException(String.format("Wait on %s", name.substring(0, 8)));
      }
      Thread.sleep(WAIT_DELAY_MS);
    }

    // consider last entry
    String value = values[values.length - 1];
    directoryRemove(name, value);

    if (value.isEmpty()) {
      throw new Exception("Invalid response");
    }
    if (log.isDebugEnabled()) {
      log.debug("<= " + name + " (" + elapsedTime + " ms)");
    }
    return value;
  }

  public static String encodeDirectory(String name) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(name.getBytes(Charsets.UTF_8));
    String result = Hex.toHexString(hash);
    if (result.isEmpty()) {
      throw new Exception("Invalid encodeDirectory value");
    }
    return result;
  }
}
