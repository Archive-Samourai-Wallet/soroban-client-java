package com.samourai.soroban.client.meeting;

import com.samourai.http.client.IHttpClient;
import com.samourai.soroban.client.dialog.RpcDialog;
import com.samourai.soroban.client.dialog.User;
import com.samourai.soroban.client.rpc.RpcClient;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.bip47.rpc.java.Bip47UtilJava;
import com.samourai.wallet.cahoots.CahootsType;
import com.samourai.wallet.soroban.client.SorobanMessage;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanMeetingService {
  private static final Logger log = LoggerFactory.getLogger(SorobanMeetingService.class);

  private RpcClient rpc;
  private BIP47Wallet bip47w;

  public SorobanMeetingService(
      NetworkParameters params, BIP47Wallet bip47w, IHttpClient httpClient) {
    this.bip47w = bip47w;
    this.rpc = new RpcClient(httpClient, params);
  }

  private PaymentCode getMyPaymentCode() {
    return Bip47UtilJava.getInstance().getPaymentCode(bip47w);
  }

  public Observable<SorobanResponseMessage> meetingRequest(
      PaymentCode paymentCodeCounterParty,
      String description,
      CahootsType type,
      final long timeoutMs)
      throws Exception {
    User user = new User(bip47w);

    // send request
    final RpcDialog dialog = new RpcDialog(rpc, user, paymentCodeCounterParty.toString());
    SorobanMessage request =
        new SorobanRequestMessage(getMyPaymentCode().toString(), description, type);
    if (log.isDebugEnabled()) {
      log.debug("[initiator] meeting request: " + request);
    }
    return dialog
        .send(request)
        .switchMap(
            new Function<Object, Observable<SorobanResponseMessage>>() {
              @Override
              public Observable<SorobanResponseMessage> apply(Object onRequestSent)
                  throws Exception {
                if (log.isDebugEnabled()) {
                  log.debug("[initiator] meeting request sent, listening for response");
                }
                // receive response
                return dialog
                    .receive(timeoutMs)
                    .map(
                        new Function<String, SorobanResponseMessage>() {
                          @Override
                          public SorobanResponseMessage apply(String payload) throws Exception {
                            SorobanResponseMessage response = SorobanResponseMessage.parse(payload);
                            if (log.isDebugEnabled()) {
                              log.debug("[initiator] meeting response: " + response);
                            }
                            return response;
                          }
                        });
              }
            });
  }

  public Observable<SorobanRequestMessage> meetingListen(long timeoutMs) throws Exception {
    User user = new User(bip47w);

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
                  log.debug("[contributor] meeting request: " + request);
                }
                return request;
              }
            });
  }

  public Observable<SorobanResponseMessage> meetingResponse(
      SorobanRequestMessage request, boolean accept) throws Exception {
    User user = new User(bip47w);

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
}
