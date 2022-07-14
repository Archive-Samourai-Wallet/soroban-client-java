package com.samourai.soroban.client.meeting;

import com.samourai.soroban.client.dialog.RpcDialog;
import com.samourai.soroban.client.rpc.RpcService;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.cahoots.CahootsType;
import com.samourai.wallet.cahoots.CahootsWallet;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanMeetingService {
  private static final Logger log = LoggerFactory.getLogger(SorobanMeetingService.class);

  private RpcService rpcService;

  public SorobanMeetingService(RpcService rpcService) {
    this.rpcService = rpcService;
  }

  public Observable<SorobanRequestMessage> sendMeetingRequest(
      CahootsWallet cahootsWallet, PaymentCode paymentCodePartner, CahootsType type)
      throws Exception {
    // send request
    RpcDialog dialog = rpcService.createRpcDialog(cahootsWallet, paymentCodePartner.toString());
    final SorobanRequestMessage request = new SorobanRequestMessage(type);
    if (log.isDebugEnabled()) {
      log.debug("[initiator] meeting request sending: " + request);
    }
    return dialog
        .sendWithSender(request, cahootsWallet.getPaymentCode(), paymentCodePartner)
        .map((Function<Object, SorobanRequestMessage>) o -> request);
  }

  public Observable<SorobanRequestMessage> receiveMeetingRequest(
      CahootsWallet cahootsWallet, long timeoutMs) throws Exception {
    RpcDialog dialog =
        rpcService.createRpcDialog(cahootsWallet, cahootsWallet.getPaymentCode().toString());
    if (log.isDebugEnabled()) {
      log.debug("[contributor] listening");
    }
    return dialog
        .receiveWithSender(timeoutMs)
        .map(
            message -> {
              String sender = message.getSender();
              SorobanRequestMessage request = SorobanRequestMessage.parse(message.getPayload());
              request.setSender(sender); // set sender information
              if (log.isDebugEnabled()) {
                log.debug("[contributor] meeting request received: " + request);
              }
              return request;
            });
  }

  public Observable<SorobanResponseMessage> sendMeetingResponse(
      CahootsWallet cahootsWallet,
      PaymentCode paymentCodePartner,
      SorobanRequestMessage request,
      boolean accept)
      throws Exception {
    RpcDialog dialog = rpcService.createRpcDialog(cahootsWallet, request);
    final SorobanResponseMessage response = new SorobanResponseMessage(accept);
    return dialog
        .send(response, paymentCodePartner)
        .map(
            (Function<Object, SorobanResponseMessage>)
                o -> {
                  if (log.isDebugEnabled()) {
                    log.debug("[contributor] meeting response sent: " + response);
                  }
                  return response;
                });
  }

  public Observable<SorobanResponseMessage> receiveMeetingResponse(
      CahootsWallet cahootsWallet,
      PaymentCode paymentCodePartner,
      SorobanRequestMessage request,
      final long timeoutMs)
      throws Exception {
    // send request
    final RpcDialog dialog = rpcService.createRpcDialog(cahootsWallet, request);
    return dialog
        .receive(paymentCodePartner, timeoutMs)
        .map(
            payload -> {
              SorobanResponseMessage response = SorobanResponseMessage.parse(payload);
              if (log.isDebugEnabled()) {
                log.debug("[initiator] meeting response received: " + response);
              }
              return response;
            });
  }
}
