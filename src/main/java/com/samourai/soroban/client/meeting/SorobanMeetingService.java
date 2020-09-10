package com.samourai.soroban.client.meeting;

import com.samourai.http.client.IHttpClient;
import com.samourai.soroban.client.dialog.RpcDialog;
import com.samourai.soroban.client.dialog.User;
import com.samourai.soroban.client.rpc.RpcClient;
import com.samourai.wallet.bip47.BIP47UtilGeneric;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.cahoots.CahootsType;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanMeetingService {
  private static final Logger log = LoggerFactory.getLogger(SorobanMeetingService.class);

  private BIP47UtilGeneric bip47Util;
  private RpcClient rpc;
  private BIP47Wallet bip47w;

  public SorobanMeetingService(
      BIP47UtilGeneric bip47Util,
      NetworkParameters params,
      BIP47Wallet bip47w,
      IHttpClient httpClient) {
    this.bip47Util = bip47Util;
    this.bip47w = bip47w;
    this.rpc = new RpcClient(httpClient, params);
  }

  private PaymentCode getMyPaymentCode() {
    return bip47Util.getPaymentCode(bip47w);
  }

  public Observable<SorobanRequestMessage> sendMeetingRequest(
      PaymentCode paymentCodeCounterParty, String description, CahootsType type) throws Exception {
    User user = new User(bip47Util, bip47w);

    // send request
    final RpcDialog dialog = new RpcDialog(rpc, user, paymentCodeCounterParty.toString());
    final SorobanRequestMessage request =
        new SorobanRequestMessage(getMyPaymentCode().toString(), description, type);
    if (log.isDebugEnabled()) {
      log.debug("[initiator] meeting request sending: " + request);
    }
    return dialog
        .send(request)
        .map(
            new Function<Object, SorobanRequestMessage>() {
              @Override
              public SorobanRequestMessage apply(Object o) throws Exception {
                return request;
              }
            });
  }

  public Observable<SorobanRequestMessage> receiveMeetingRequest(long timeoutMs) throws Exception {
    User user = new User(bip47Util, bip47w);

    RpcDialog dialog = new RpcDialog(rpc, user, getMyPaymentCode().toString());
    if (log.isDebugEnabled()) {
      log.debug("[contributor] listening");
    }
    return dialog
        .receive(timeoutMs)
        .map(
            new Function<String, SorobanRequestMessage>() {
              @Override
              public SorobanRequestMessage apply(String payload) throws Exception {
                SorobanRequestMessage request = SorobanRequestMessage.parse(payload);
                if (log.isDebugEnabled()) {
                  log.debug("[contributor] meeting request received: " + request);
                }
                return request;
              }
            });
  }

  public Observable<SorobanResponseMessage> sendMeetingResponse(
      SorobanRequestMessage request, boolean accept) throws Exception {
    User user = new User(bip47Util, bip47w);

    RpcDialog dialog = new RpcDialog(rpc, user, request);
    final SorobanResponseMessage response = new SorobanResponseMessage(accept);
    return dialog
        .send(response)
        .map(
            new Function<Object, SorobanResponseMessage>() {
              @Override
              public SorobanResponseMessage apply(Object o) throws Exception {
                if (log.isDebugEnabled()) {
                  log.debug("[contributor] meeting response sent: " + response);
                }
                return response;
              }
            });
  }

  public Observable<SorobanResponseMessage> receiveMeetingResponse(
      SorobanRequestMessage request, final long timeoutMs) throws Exception {
    User user = new User(bip47Util, bip47w);

    // send request
    final RpcDialog dialog = new RpcDialog(rpc, user, request);
    return dialog
        .receive(timeoutMs)
        .map(
            new Function<String, SorobanResponseMessage>() {
              @Override
              public SorobanResponseMessage apply(String payload) throws Exception {
                SorobanResponseMessage response = SorobanResponseMessage.parse(payload);
                if (log.isDebugEnabled()) {
                  log.debug("[initiator] meeting response received: " + response);
                }
                return response;
              }
            });
  }
}
