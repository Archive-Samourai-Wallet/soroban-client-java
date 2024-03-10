package com.samourai.soroban.client.meeting;

import com.samourai.soroban.client.dialog.RpcDialog;
import com.samourai.soroban.client.rpc.RpcSession;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.cahoots.CahootsType;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanMeetingService {
  private static final Logger log = LoggerFactory.getLogger(SorobanMeetingService.class);

  public SorobanMeetingService() {}

  public Single<SorobanRequestMessage> sendMeetingRequest(
      RpcSession rpcSession, PaymentCode paymentCodePartner, CahootsType type) {
    try {
      String info = "[initiator]";
      // send request
      RpcDialog dialog = rpcSession.createRpcDialog(paymentCodePartner.toString());
      // sender is wrapped within SorobanMessageWithSender
      final SorobanRequestMessage request = new SorobanRequestMessage(type, null);
      if (log.isDebugEnabled()) {
        log.debug(info + "sending meeting request: " + request);
      }
      return dialog.sendWithSender(request, paymentCodePartner).toSingle(() -> request);
    } catch (Exception e) {
      return Single.error(e);
    }
  }

  public SorobanRequestMessage receiveMeetingRequest(RpcSession rpcSession, long timeoutMs)
      throws Exception {
    String info = "[counterparty]";
    PaymentCode paymentCodeCounterparty =
        rpcSession.getRpcWallet().getBip47Account().getPaymentCode();
    RpcDialog dialog = rpcSession.createRpcDialog(paymentCodeCounterparty.toString());
    if (log.isDebugEnabled()) {
      log.debug(info + "listening for meeting request...");
    }
    SorobanMessageWithSender message = dialog.receiveWithSender(timeoutMs);
    PaymentCode sender = message.getSender();
    SorobanRequestMessage request = SorobanRequestMessage.parse(message.getPayload(), sender);
    if (log.isDebugEnabled()) {
      log.debug(info + "meeting request received: " + request);
    }
    return request;
  }

  public Single<SorobanResponseMessage> sendMeetingResponse(
      RpcSession rpcSession, SorobanRequestMessage request, boolean accept) {
    try {
      String info = "[counterparty]";
      RpcDialog dialog = rpcSession.createRpcDialog(request.toPayload());
      final SorobanResponseMessage response = new SorobanResponseMessage(accept);
      PaymentCode paymentCodePartner = request.getSender();
      return dialog
          .send(response, paymentCodePartner)
          .toSingle(
              () -> {
                if (log.isDebugEnabled()) {
                  log.debug(info + "meeting response sent: " + response);
                }
                return response;
              });
    } catch (Exception e) {
      log.error("sendMeetingResponse failed", e);
      return Single.error(e);
    }
  }

  public SorobanResponseMessage receiveMeetingResponse(
      RpcSession rpcSession,
      PaymentCode paymentCodePartner,
      SorobanRequestMessage request,
      final int timeoutMs)
      throws Exception {
    // send request
    String info = "[initiator]";
    final RpcDialog dialog = rpcSession.createRpcDialog(request.toPayload());
    String payload = dialog.receive(paymentCodePartner, timeoutMs);
    SorobanResponseMessage response = SorobanResponseMessage.parse(payload);
    return response;
  }
}
