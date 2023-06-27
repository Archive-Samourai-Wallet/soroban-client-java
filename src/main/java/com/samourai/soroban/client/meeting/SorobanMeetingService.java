package com.samourai.soroban.client.meeting;

import com.samourai.soroban.client.dialog.RpcDialog;
import com.samourai.soroban.client.rpc.RpcClientService;
import com.samourai.soroban.client.rpc.RpcSession;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.cahoots.CahootsType;
import com.samourai.wallet.cahoots.CahootsWallet;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanMeetingService {
  private static final Logger log = LoggerFactory.getLogger(SorobanMeetingService.class);

  private RpcClientService rpcClientService;

  public SorobanMeetingService(RpcClientService rpcClientService) {
    this.rpcClientService = rpcClientService;
  }

  public Single<SorobanRequestMessage> sendMeetingRequest(
      CahootsWallet cahootsWallet, PaymentCode paymentCodePartner, CahootsType type)
      throws Exception {
    String info = "[initiator]";
    // send request
    RpcSession rpcSession = rpcClientService.getRpcSession(info);
    RpcDialog dialog =
        rpcSession.createRpcDialog(
            cahootsWallet.getRpcWallet().getEncrypter(), paymentCodePartner.toString());
    final SorobanRequestMessage request = new SorobanRequestMessage(type);
    if (log.isDebugEnabled()) {
      log.debug(info + "sending meeting request: " + request);
    }
    return dialog.sendWithSender(request, paymentCodePartner).map(payload -> request);
  }

  public Single<SorobanRequestMessage> receiveMeetingRequest(
      CahootsWallet cahootsWallet, long timeoutMs) throws Exception {
    String info = "[counterparty]";
    RpcSession rpcSession = rpcClientService.getRpcSession(info);
    RpcDialog dialog =
        rpcSession.createRpcDialog(
            cahootsWallet.getRpcWallet().getEncrypter(), cahootsWallet.getPaymentCode().toString());
    if (log.isDebugEnabled()) {
      log.debug(info + "listening for meeting request...");
    }
    return dialog
        .receiveWithSender(timeoutMs)
        .map(
            message -> {
              String sender = message.getSender();
              SorobanRequestMessage request = SorobanRequestMessage.parse(message.getPayload());
              request.setSender(sender); // set sender information
              if (log.isDebugEnabled()) {
                log.debug(info + "meeting request received: " + request);
              }
              return request;
            });
  }

  public Single<SorobanResponseMessage> sendMeetingResponse(
      CahootsWallet cahootsWallet, SorobanRequestMessage request, boolean accept) throws Exception {
    String info = "[counterparty]";
    RpcSession rpcSession = rpcClientService.getRpcSession(info);
    RpcDialog dialog =
        rpcSession.createRpcDialog(
            cahootsWallet.getRpcWallet().getEncrypter(), request.toPayload());
    final SorobanResponseMessage response = new SorobanResponseMessage(accept);
    PaymentCode paymentCodePartner = new PaymentCode(request.getSender());
    return dialog
        .send(response, paymentCodePartner)
        .map(
            payload -> {
              if (log.isDebugEnabled()) {
                log.debug(info + "meeting response sent: " + response);
              }
              return response;
            });
  }

  public Single<SorobanResponseMessage> receiveMeetingResponse(
      CahootsWallet cahootsWallet,
      PaymentCode paymentCodePartner,
      SorobanRequestMessage request,
      final long timeoutMs)
      throws Exception {
    // send request
    String info = "[initiator]";
    RpcSession rpcSession = rpcClientService.getRpcSession(info);
    final RpcDialog dialog =
        rpcSession.createRpcDialog(
            cahootsWallet.getRpcWallet().getEncrypter(), request.toPayload());
    return dialog
        .receive(paymentCodePartner, timeoutMs)
        .map(
            payload -> {
              SorobanResponseMessage response = SorobanResponseMessage.parse(payload);
              if (log.isDebugEnabled()) {
                log.debug(info + "meeting response received: " + response);
              }
              return response;
            });
  }
}
