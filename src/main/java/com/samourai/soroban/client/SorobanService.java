package com.samourai.soroban.client;

import com.samourai.http.client.IHttpClient;
import com.samourai.soroban.client.dialog.RpcDialog;
import com.samourai.soroban.client.dialog.User;
import com.samourai.soroban.client.rpc.RpcClient;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.soroban.client.SorobanMessage;
import com.samourai.wallet.soroban.client.SorobanMessageService;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanService {
  private static final Logger log = LoggerFactory.getLogger(SorobanService.class);

  private NetworkParameters params;
  private RpcClient rpc;
  private BIP47Wallet bip47w;

  private boolean exit;

  public SorobanService(NetworkParameters params, BIP47Wallet bip47w, IHttpClient httpClient) {
    this.params = params;
    this.bip47w = bip47w;
    this.exit = false;
    this.rpc = new RpcClient(httpClient, params);
  }

  public Observable<SorobanMessage> initiator(
      final int account,
      final SorobanMessageService messageService,
      final PaymentCode paymentCodeCounterParty,
      final long timeoutMs,
      final SorobanMessage message)
      throws Exception {
    final BehaviorSubject onMessage = BehaviorSubject.create();
    Thread t =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                try {
                  User user = new User(bip47w);
                  String initialDirectory =
                      user.getMeeetingAddressSend(paymentCodeCounterParty, params)
                          .getBech32AsString();

                  RpcDialog dialog = new RpcDialog(rpc, user, initialDirectory);
                  dialog(
                      account, messageService, dialog, timeoutMs, message, "INITIATOR", onMessage);
                  onMessage.onComplete();
                } catch (Exception e) {
                  onMessage.onError(e);
                }
              }
            });
    t.setName("soroban-initiator");
    t.start();
    return onMessage;
  }

  public Observable<SorobanMessage> contributor(
      final int account,
      final SorobanMessageService messageService,
      final PaymentCode paymentCodeInitiator,
      final long timeoutMs)
      throws Exception {
    final BehaviorSubject onMessage = BehaviorSubject.create();
    Thread t =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                try {
                  User user = new User(bip47w);
                  String initialDirectory =
                      user.getMeeetingAddressReceive(paymentCodeInitiator, params)
                          .getBech32AsString();

                  RpcDialog dialog = new RpcDialog(rpc, user, initialDirectory);

                  String info = "CONTRIBUTOR";
                  SorobanMessage message =
                      receive(messageService, dialog, timeoutMs).blockingSingle();
                  onMessage.onNext(message);
                  if (log.isDebugEnabled()) {
                    log.debug(info + " #(0) <= " + message.toString());
                  }
                  if (message.isLastMessage()) {
                    if (log.isDebugEnabled()) {
                      log.debug(info + " #(0) done.");
                    }
                    onMessage.onComplete();
                    return;
                  }
                  SorobanMessage response = messageService.reply(account, message);
                  dialog(
                      account,
                      messageService,
                      dialog,
                      timeoutMs,
                      response,
                      "CONTRIBUTOR",
                      onMessage);
                  onMessage.onComplete();
                } catch (Exception e) {
                  onMessage.onError(e);
                }
              }
            });
    t.setName("soroban-contributor");
    t.start();
    return onMessage;
  }

  private SorobanMessage dialog(
      int account,
      SorobanMessageService messageService,
      RpcDialog dialog,
      long timeoutMs,
      SorobanMessage message,
      String info,
      Subject<SorobanMessage> onMessage)
      throws Exception {
    int i = 0;
    while (true) {
      if (exit) {
        if (log.isDebugEnabled()) {
          log.debug(info + " #" + i + " => forced exit");
        }
        break;
      }
      // send first message
      if (log.isDebugEnabled()) {
        log.debug(info + " #" + i + " => " + message.toString());
      }
      if (onMessage != null) {
        onMessage.onNext(message);
      }
      dialog.send(message).blockingSingle();

      if (message.isLastMessage()) {
        // done
        if (log.isDebugEnabled()) {
          log.debug(info + " #" + i + " done.");
        }
        break;
      }

      // receive response
      message = receive(messageService, dialog, timeoutMs).blockingSingle();
      if (onMessage != null) {
        onMessage.onNext(message);
      }
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
      message = messageService.reply(account, message);
      i++;
    }
    return message;
  }

  private Observable<SorobanMessage> receive(
      final SorobanMessageService messageService, RpcDialog dialog, long timeoutMs)
      throws Exception {
    return dialog
        .receive(timeoutMs)
        .map(
            new Function<String, SorobanMessage>() {
              @Override
              public SorobanMessage apply(String payload) throws Exception {
                SorobanMessage response = messageService.parse(payload);
                return response;
              }
            });
  }

  public void close() {
    exit = true;
  }
}
