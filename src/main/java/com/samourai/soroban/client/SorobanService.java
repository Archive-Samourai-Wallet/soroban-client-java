package com.samourai.soroban.client;

import com.samourai.soroban.client.dialog.RpcDialog;
import com.samourai.soroban.client.dialog.User;
import com.samourai.soroban.client.rpc.RpcClient;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.soroban.client.SorobanMessage;
import com.samourai.wallet.soroban.client.SorobanMessageService;
import com.samourai.wallet.soroban.client.SorobanServer;
import com.samourai.wallet.util.FormatsUtilGeneric;
import org.apache.http.client.protocol.HttpClientContext;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanService {
  private static final Logger log = LoggerFactory.getLogger(SorobanService.class);

  private NetworkParameters params;
  private RpcClient rpc;
  private SorobanMessageService messageService;
  private BIP47Wallet bip47w;

  public SorobanService(
      NetworkParameters params, BIP47Wallet bip47w, SorobanMessageService messageService) {
    this.params = params;
    this.messageService = messageService;
    this.bip47w = bip47w;
    this.rpc = computeRpcClient(params);
  }

  private static RpcClient computeRpcClient(NetworkParameters params) {
    String url = SorobanServer.get(FormatsUtilGeneric.getInstance().isTestNet(params)).getUrl();
    RpcClient rpc = new RpcClient(url);
    return rpc;
  }

  public SorobanMessage initiator(PaymentCode paymentCodeCounterParty, SorobanMessage message)
      throws Exception {
    User user = new User(bip47w);
    RpcDialog dialog = RpcDialog.initiator(rpc, user, paymentCodeCounterParty, params);

    SorobanMessage lastMessage = dialog(dialog, message, "INITIATOR");
    return lastMessage;
  }

  public SorobanMessage contributor(PaymentCode paymentCodeInitiator) throws Exception {
    User user = new User(bip47w);
    RpcDialog dialog = RpcDialog.contributor(rpc, user, paymentCodeInitiator, params);

    String info = "CONTRIBUTOR";
    SorobanMessage message = receive(dialog);
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
    SorobanMessage lastMessage = dialog(dialog, response, "CONTRIBUTOR");
    return lastMessage;
  }

  private SorobanMessage dialog(RpcDialog dialog, SorobanMessage message, String info)
      throws Exception {
    int i = 0;
    while (true) {
      // send first message
      if (log.isDebugEnabled()) {
        log.debug(info + " #" + i + " => " + message.toString());
      }
      dialog.send(message.toPayload());

      if (message.isLastMessage()) {
        // done
        if (log.isDebugEnabled()) {
          log.debug(info + " #" + i + " done.");
        }
        break;
      }

      // receive response
      message = receive(dialog);
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

  private SorobanMessage receive(RpcDialog dialog) throws Exception {
    String payload = dialog.receive();
    SorobanMessage response = messageService.parse(payload);
    return response;
  }

  public void close() throws Exception {
    rpc.close();
  }

  public HttpClientContext getHttpClientContext() {
    return rpc.getHttpClientContext();
  }
}
