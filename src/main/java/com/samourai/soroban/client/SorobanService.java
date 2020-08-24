package com.samourai.soroban.client;

import com.samourai.http.client.IHttpClient;
import com.samourai.soroban.client.dialog.RpcDialog;
import com.samourai.soroban.client.dialog.User;
import com.samourai.soroban.client.rpc.RpcClient;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.soroban.client.SorobanMessage;
import com.samourai.wallet.soroban.client.SorobanMessageService;
import com.samourai.wallet.soroban.client.SorobanServer;
import com.samourai.wallet.util.FormatsUtilGeneric;
import io.reactivex.subjects.Subject;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanService {
  private static final Logger log = LoggerFactory.getLogger(SorobanService.class);
  private static final String ENDPOINT_RPC = "/rpc";

  private NetworkParameters params;
  private RpcClient rpc;
  private SorobanMessageService messageService;
  private BIP47Wallet bip47w;
  private int timeoutMs;

  private boolean exit;

  public SorobanService(
      NetworkParameters params,
      BIP47Wallet bip47w,
      SorobanMessageService messageService,
      IHttpClient httpClient,
      int timeoutMs) {
    this.params = params;
    this.messageService = messageService;
    this.bip47w = bip47w;
    this.exit = false;
    String url =
        SorobanServer.get(FormatsUtilGeneric.getInstance().isTestNet(params)).getUrl()
            + ENDPOINT_RPC;
    this.rpc = new RpcClient(httpClient, url);
    this.timeoutMs = timeoutMs;
  }

  public SorobanMessage initiator(
      PaymentCode paymentCodeCounterParty,
      SorobanMessage message,
      Subject<SorobanMessage> onMessage)
      throws Exception {
    User user = new User(bip47w);
    RpcDialog dialog = RpcDialog.initiator(rpc, user, paymentCodeCounterParty, params);

    SorobanMessage lastMessage = dialog(dialog, message, "INITIATOR", onMessage);
    return lastMessage;
  }

  public SorobanMessage contributor(
      PaymentCode paymentCodeInitiator, long timeoutMs, Subject<SorobanMessage> onMessage)
      throws Exception {
    User user = new User(bip47w);
    RpcDialog dialog = RpcDialog.contributor(rpc, user, paymentCodeInitiator, params);

    String info = "CONTRIBUTOR";
    SorobanMessage message = receive(dialog, timeoutMs);
    onMessage.onNext(message);
    if (log.isDebugEnabled()) {
      log.debug(info + " #(0) <= " + message.toString());
    }
    if (message.isLastMessage()) {
      if (log.isDebugEnabled()) {
        log.debug(info + " #(0) done.");
      }
      return message;
    }
    SorobanMessage response = messageService.reply(message);
    SorobanMessage lastMessage = dialog(dialog, response, "CONTRIBUTOR", onMessage);
    return lastMessage;
  }

  private SorobanMessage dialog(
      RpcDialog dialog, SorobanMessage message, String info, Subject<SorobanMessage> onMessage)
      throws Exception {
    int i = 0;
    while (true) {
      if (exit) {
        if (log.isDebugEnabled()) {
          log.debug("=> forced exit");
        }
        break;
      }
      // send first message
      if (log.isDebugEnabled()) {
        log.debug(info + " #" + i + " => " + message.toString());
      }
      onMessage.onNext(message);
      dialog.send(message.toPayload());

      if (message.isLastMessage()) {
        // done
        if (log.isDebugEnabled()) {
          log.debug(info + " #" + i + " done.");
        }
        break;
      }

      // receive response
      message = receive(dialog, timeoutMs);
      onMessage.onNext(message);
      if (log.isDebugEnabled()) {
        log.debug(info + " #" + i + " <= " + message.toString());
      }
      if (message.isLastMessage()) {
        // done
        if (log.isDebugEnabled()) {
          log.debug(info + " #" + i + " done.");
        }
        break;
      }

      // prepare reply
      message = messageService.reply(message);
      i++;
    }
    return message;
  }

  private SorobanMessage receive(RpcDialog dialog, long timeoutMs) throws Exception {
    String payload = dialog.receive(timeoutMs);
    SorobanMessage response = messageService.parse(payload);
    return response;
  }

  public void close() {
    exit = true;
  }
}
