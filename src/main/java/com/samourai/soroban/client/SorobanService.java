package com.samourai.soroban.client;

import com.samourai.http.client.IHttpClient;
import com.samourai.soroban.client.dialog.RpcDialog;
import com.samourai.soroban.client.dialog.User;
import com.samourai.soroban.client.rpc.RpcClient;
import com.samourai.wallet.bip47.BIP47UtilGeneric;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.soroban.cahoots.CahootsContext;
import com.samourai.wallet.soroban.client.SorobanInteraction;
import com.samourai.wallet.soroban.client.SorobanMessage;
import com.samourai.wallet.soroban.client.SorobanMessageService;
import com.samourai.wallet.soroban.client.SorobanReply;
import io.reactivex.Observable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Function;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import java.util.concurrent.Callable;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanService {
  private static final Logger log = LoggerFactory.getLogger(SorobanService.class);

  private NetworkParameters params;
  private RpcClient rpc;
  private User user;
  private Subject<SorobanMessage> interactiveMessageProvider;
  private Subject<SorobanInteraction> onInteraction;

  public SorobanService(
      BIP47UtilGeneric bip47Util,
      NetworkParameters params,
      String provider,
      BIP47Wallet bip47w,
      IHttpClient httpClient) {
    this.params = params;
    this.rpc = new RpcClient(httpClient, params);
    this.user = new User(bip47Util, bip47w, params, provider);
    this.interactiveMessageProvider = BehaviorSubject.create();
    this.onInteraction = BehaviorSubject.create();
  }

  public Observable<SorobanMessage> initiator(
      final int account,
      final CahootsContext cahootsContext,
      final SorobanMessageService messageService,
      final PaymentCode paymentCodeCounterParty,
      final long timeoutMs,
      final SorobanMessage message)
      throws Exception {
    final BehaviorSubject<SorobanMessage> onMessage = BehaviorSubject.create();
    Thread t =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                try {
                  String initialDirectory =
                      user.getMeeetingAddressSend(paymentCodeCounterParty, params)
                          .getBech32AsString();

                  final RpcDialog dialog = new RpcDialog(rpc, user, initialDirectory);
                  onMessage.doOnDispose(
                      new Action() {
                        @Override
                        public void run() throws Exception {
                          dialog.close();
                        }
                      });
                  dialog(
                      account,
                      cahootsContext,
                      messageService,
                      dialog,
                      paymentCodeCounterParty,
                      timeoutMs,
                      message,
                      "INITIATOR",
                      onMessage);
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
      final CahootsContext cahootsContext,
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
                  String initialDirectory =
                      user.getMeeetingAddressReceive(paymentCodeInitiator, params)
                          .getBech32AsString();

                  final RpcDialog dialog = new RpcDialog(rpc, user, initialDirectory);
                  onMessage.doOnDispose(
                      new Action() {
                        @Override
                        public void run() throws Exception {
                          dialog.close();
                        }
                      });

                  String info = "CONTRIBUTOR";
                  SorobanMessage message =
                      receive(messageService, dialog, paymentCodeInitiator, timeoutMs)
                          .blockingSingle();
                  onMessage.onNext(message);
                  if (log.isDebugEnabled()) {
                    log.debug(info + " #(0) <= " + message.toString());
                  }
                  if (message.isDone()) {
                    if (log.isDebugEnabled()) {
                      log.debug(info + " #(0) done.");
                    }
                    onMessage.onComplete();
                    return;
                  }

                  SorobanMessage response =
                      (SorobanMessage)
                          safeReply(
                              messageService,
                              account,
                              cahootsContext,
                              message,
                              dialog,
                              paymentCodeInitiator);
                  dialog(
                      account,
                      cahootsContext,
                      messageService,
                      dialog,
                      paymentCodeInitiator,
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

  private SorobanReply safeReply(
      SorobanMessageService messageService,
      int account,
      CahootsContext cahootsContext,
      SorobanMessage message,
      RpcDialog dialog,
      PaymentCode paymentCodePartner)
      throws Exception {
    try {
      return messageService.reply(account, cahootsContext, message);
    } catch (Exception e) {
      // send error
      dialog.sendError("Dialog failed", paymentCodePartner).subscribe();
      throw e;
    }
  }

  private SorobanMessage dialog(
      int account,
      CahootsContext cahootsContext,
      SorobanMessageService messageService,
      RpcDialog dialog,
      PaymentCode paymentCodePartner,
      long timeoutMs,
      SorobanMessage message,
      String info,
      Subject<SorobanMessage> onMessage)
      throws Exception {
    int i = 0;
    while (true) {
      // send first message
      if (log.isDebugEnabled()) {
        log.debug(info + " #" + i + " => " + message.toString());
      }
      if (onMessage != null) {
        onMessage.onNext(message);
      }
      dialog.send(message, paymentCodePartner).blockingSingle();

      if (message.isDone()) {
        // done
        if (log.isDebugEnabled()) {
          log.debug(info + " #" + i + " done.");
        }
        break;
      }

      // receive response
      message = receive(messageService, dialog, paymentCodePartner, timeoutMs).blockingSingle();
      if (onMessage != null) {
        onMessage.onNext(message);
      }
      if (log.isDebugEnabled()) {
        log.debug(info + " #" + i + " <= " + message.toString());
      }

      if (message.isDone()) {
        // done
        if (log.isDebugEnabled()) {
          log.debug(info + " #" + i + " done.");
        }
        break;
      }

      // prepare reply
      SorobanReply reply =
          safeReply(messageService, account, cahootsContext, message, dialog, paymentCodePartner);

      if (reply instanceof SorobanInteraction) {
        // wrap interaction for Soroban
        SorobanInteraction interaction = computeOnlineInteraction((SorobanInteraction) reply);
        if (log.isDebugEnabled()) {
          log.debug(info + " #" + i + " => [INTERACTIVE] ... >? " + interaction);
        }
        onInteraction.onNext(interaction);

        // wait for interaction confirmation
        message = interactiveMessageProvider.blockingNext().iterator().next();
        if (log.isDebugEnabled()) {
          log.debug(info + " #" + i + " => [INTERACTIVE] " + message.toString());
        }
      } else {
        // direct reply
        message = (SorobanMessage) reply;
      }
      i++;
    }
    return message;
  }

  private SorobanInteraction computeOnlineInteraction(final SorobanInteraction interaction) {
    Callable<SorobanMessage> onAccept =
        new Callable<SorobanMessage>() {
          @Override
          public SorobanMessage call() throws Exception {
            SorobanMessage acceptMessage = interaction.accept();
            replyInteractive(acceptMessage);
            return acceptMessage;
          }
        };
    SorobanInteraction onlineInteraction =
        new SorobanInteraction(
            interaction.getRequest(), interaction.getTypeInteraction(), onAccept);
    return onlineInteraction;
  }

  private Observable<SorobanMessage> receive(
      final SorobanMessageService messageService,
      RpcDialog dialog,
      PaymentCode paymentCodePartner,
      long timeoutMs)
      throws Exception {
    return dialog
        .receive(paymentCodePartner, timeoutMs)
        .map(
            new Function<String, SorobanMessage>() {
              @Override
              public SorobanMessage apply(String payload) throws Exception {
                SorobanMessage response = messageService.parse(payload);
                return response;
              }
            });
  }

  private void replyInteractive(SorobanMessage message) {
    if (log.isDebugEnabled()) {
      log.debug(" => replyInteractive");
    }
    interactiveMessageProvider.onNext(message);
  }

  public Subject<SorobanInteraction> getOnInteraction() {
    return onInteraction;
  }
}
