package com.samourai.http.client;

import com.google.common.base.Charsets;
import com.samourai.wallet.api.backend.beans.HttpException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.FormContentProvider;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.Fields;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaHttpClient extends JacksonHttpClient {
  private static final Logger log = LoggerFactory.getLogger(JavaHttpClient.class);

  private HttpClient httpClient;
  private long requestTimeout;

  public JavaHttpClient(long requestTimeout) {
    this(computeHttpClient(), requestTimeout);
  }

  public JavaHttpClient(HttpClient httpClient, long requestTimeout) {
    super();
    this.httpClient = httpClient;
    this.requestTimeout = requestTimeout;
  }

  protected static HttpClient computeHttpClient() {
    // we use jetty for proxy SOCKS support
    HttpClient jettyHttpClient = new HttpClient(new SslContextFactory());
    jettyHttpClient.setUserAgentField(new HttpField(HttpHeader.USER_AGENT, "soroban-client"));
    return jettyHttpClient;
  }

  @Override
  public void connect() throws Exception {
    if (!httpClient.isRunning()) {
      httpClient.start();
    }
  }

  public void restart() {
    try {
      if (log.isDebugEnabled()) {
        log.debug("restart");
      }
      if (httpClient.isRunning()) {
        httpClient.stop();
      }
      httpClient.start();
    } catch (Exception e) {
      log.error("", e);
    }
  }

  @Override
  protected String requestJsonGet(String urlStr, Map<String, String> headers, boolean async)
      throws Exception {
    Request req = computeHttpRequest(urlStr, HttpMethod.GET, headers);
    return requestJson(req);
  }

  @Override
  protected String requestJsonPost(String urlStr, Map<String, String> headers, String jsonBody)
      throws Exception {
    Request req = computeHttpRequest(urlStr, HttpMethod.POST, headers);
    req.content(new StringContentProvider("application/json", jsonBody, Charsets.UTF_8));
    return requestJson(req);
  }

  @Override
  protected String requestJsonPostUrlEncoded(
      String urlStr, Map<String, String> headers, Map<String, String> body) throws Exception {
    Request req = computeHttpRequest(urlStr, HttpMethod.POST, headers);
    req.content(new FormContentProvider(computeBodyFields(body)));
    return requestJson(req);
  }

  private Fields computeBodyFields(Map<String, String> body) {
    Fields fields = new Fields();
    for (Map.Entry<String, String> entry : body.entrySet()) {
      fields.put(entry.getKey(), entry.getValue());
    }
    return fields;
  }

  private String requestJson(Request req) throws Exception {
    ContentResponse response = req.send();
    if (response.getStatus() != HttpStatus.OK_200) {
      String responseBody = response.getContentAsString();
      log.error(
          "Http query failed: status=" + response.getStatus() + ", responseBody=" + responseBody);
      throw new HttpException(
          new Exception("Http query failed: status=" + response.getStatus()), responseBody);
    }
    String responseContent = response.getContentAsString();
    return responseContent;
  }

  public HttpClient getJettyHttpClient() throws Exception {
    connect();
    return httpClient;
  }

  private Request computeHttpRequest(String url, HttpMethod method, Map<String, String> headers)
      throws Exception {
    if (log.isDebugEnabled()) {
      String headersStr = headers != null ? " (" + headers.keySet() + ")" : "";
      log.debug("+" + method + ": " + url + headersStr);
    }
    Request req = getJettyHttpClient().newRequest(url);
    req.method(method);
    if (headers != null) {
      for (Map.Entry<String, String> entry : headers.entrySet()) {
        req.header(entry.getKey(), entry.getValue());
      }
    }
    req.timeout(requestTimeout, TimeUnit.MILLISECONDS);
    return req;
  }
}
