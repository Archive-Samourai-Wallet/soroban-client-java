package com.samourai.soroban.client;

import com.samourai.soroban.cahoots.CahootsContext;
import com.samourai.soroban.client.dialog.RpcDialog;
import com.samourai.soroban.client.rpc.RpcService;
import com.samourai.wallet.bip47.BIP47UtilGeneric;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.cahoots.CahootsWallet;
import com.samourai.wallet.segwit.SegwitAddress;
import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanService {
  private static final Logger log = LoggerFactory.getLogger(SorobanService.class);

  private BIP47UtilGeneric bip47Util;
  private NetworkParameters params;
  private RpcService rpcService;

  public SorobanService(
      BIP47UtilGeneric bip47Util, NetworkParameters params, RpcService rpcService) {
    this.bip47Util = bip47Util;
    this.params = params;
    this.rpcService = rpcService;
  }

  public Observable<SorobanMessage> initiator(
      final CahootsContext cahootsContext,
      final SorobanMessageService messageService,
      final PaymentCode paymentCodeCounterParty,
      final long timeoutMs,
      final SorobanMessage message,
      Consumer<OnlineSorobanInteraction> onInteraction)
      throws Exception {
    SorobanInteractionHandler interactionHandler = new SorobanInteractionHandler(onInteraction);
    final BehaviorSubject<SorobanMessage> onMessage = BehaviorSubject.create();
    CahootsWallet cahootsWallet = cahootsContext.getCahootsWallet();
    Thread t =
        new Thread(
            () -> {
              RpcDialog dialogOrNull = null;
              try {
                String initialDirectory =
                    getMeeetingAddressSend(cahootsWallet, paymentCodeCounterParty, params)
                        .getBech32AsString();
                dialogOrNull = rpcService.createRpcDialog(cahootsWallet, initialDirectory);
                final RpcDialog dialog = dialogOrNull;
                closeDialogOnError(onMessage, dialog, paymentCodeCounterParty, interactionHandler);
                dialog(
                    cahootsContext,
                    messageService,
                    dialog,
                    paymentCodeCounterParty,
                    timeoutMs,
                    message,
                    "INITIATOR",
                    onMessage,
                    interactionHandler);
                onMessage.onComplete();
              } catch (Exception e) {
                log.error("INITIATOR => error", e);
                fail(e.getMessage(), onMessage, dialogOrNull, paymentCodeCounterParty);
              }
            });
    t.setName("soroban-initiator");
    t.start();
    return onMessage;
  }

  public Observable<SorobanMessage> contributor(
      final CahootsContext cahootsContext,
      final SorobanMessageService messageService,
      final PaymentCode paymentCodeInitiator,
      final long timeoutMs)
      throws Exception {
    final BehaviorSubject onMessage = BehaviorSubject.create();
    CahootsWallet cahootsWallet = cahootsContext.getCahootsWallet();
    Thread t =
        new Thread(
            () -> {
              RpcDialog dialogOrNull = null;
              try {
                String initialDirectory =
                    getMeeetingAddressReceive(cahootsWallet, paymentCodeInitiator, params)
                        .getBech32AsString();
                dialogOrNull = rpcService.createRpcDialog(cahootsWallet, initialDirectory);
                final RpcDialog dialog = dialogOrNull;
                closeDialogOnError(onMessage, dialog, paymentCodeInitiator, null);

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
                            messageService, cahootsContext, message, dialog, paymentCodeInitiator);
                dialog(
                    cahootsContext,
                    messageService,
                    dialog,
                    paymentCodeInitiator,
                    timeoutMs,
                    response,
                    "CONTRIBUTOR",
                    onMessage,
                    null);
                onMessage.onComplete();
              } catch (Exception e) {
                log.error("CONTRIBUTOR => error ", e);
                fail(e.getMessage(), onMessage, dialogOrNull, paymentCodeInitiator);
              }
            });
    t.setName("soroban-contributor");
    t.start();
    return onMessage;
  }

  private SorobanReply safeReply(
      SorobanMessageService messageService,
      CahootsContext cahootsContext,
      SorobanMessage message,
      RpcDialog dialog,
      PaymentCode paymentCodePartner)
      throws Exception {
    try {
      return messageService.reply(cahootsContext, message);
    } catch (Exception e) {
      // send error
      dialog.sendError("Dialog failed", paymentCodePartner).subscribe();
      log.error("Dialog failed", e);
      throw e;
    }
  }

  private SorobanMessage dialog(
      CahootsContext cahootsContext,
      SorobanMessageService messageService,
      RpcDialog dialog,
      PaymentCode paymentCodePartner,
      long timeoutMs,
      SorobanMessage message,
      String info,
      Subject<SorobanMessage> onMessage,
      SorobanInteractionHandler interactionHandler // only set for initiator
      ) throws Exception {
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
          safeReply(messageService, cahootsContext, message, dialog, paymentCodePartner);

      if (reply instanceof SorobanInteraction) {
        if (interactionHandler == null) {
          // should never happen
          throw new Exception("No interactionHandler provided");
        }
        // wrap interaction for Soroban
        OnlineSorobanInteraction interaction =
            computeOnlineInteraction((SorobanInteraction) reply, interactionHandler);
        if (log.isDebugEnabled()) {
          log.debug(info + " #" + i + " => [INTERACTIVE] ... >? " + interaction);
        }
        interactionHandler.getOnInteraction().onNext(interaction);

        // wait for interaction confirmation
        message =
            interactionHandler.getInteractiveMessageProvider().blockingNext().iterator().next();
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

  private OnlineSorobanInteraction computeOnlineInteraction(
      final SorobanInteraction interaction, SorobanInteractionHandler interactionHandler) {
    OnlineSorobanInteraction onlineInteraction =
        new OnlineSorobanInteraction(interaction, interactionHandler);
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
            payload -> {
              SorobanMessage response = messageService.parse(payload);
              return response;
            });
  }

  private void closeDialogOnError(
      final Subject onMessage,
      final RpcDialog dialog,
      final PaymentCode paymentCodePartner,
      SorobanInteractionHandler interactionHandler) {
    onMessage.doOnDispose(() -> fail("Canceled by user", onMessage, dialog, paymentCodePartner));
    onMessage.doOnError(
        new Consumer<Throwable>() {
          @Override
          public void accept(Throwable throwable) throws Exception {
            fail(throwable.getMessage(), onMessage, dialog, paymentCodePartner);
          }
        });
    if (interactionHandler != null) {
      interactionHandler
          .getInteractiveMessageProvider()
          .doOnError(
              new Consumer<Throwable>() {
                @Override
                public void accept(Throwable throwable) throws Exception {
                  fail(throwable.getMessage(), onMessage, dialog, paymentCodePartner);
                }
              });
    }
  }

  private void fail(
      String error, Subject onMessage, RpcDialog dialog, PaymentCode paymentCodePartner) {
    if (dialog != null) {
      dialog.sendError(error, paymentCodePartner).subscribe();
      dialog.close();
    }

    onMessage.onError(new Exception(error));
    onMessage.onComplete();
  }

  private SegwitAddress getMeeetingAddressReceive(
      CahootsWallet cahootsWallet, PaymentCode paymentCodeCounterparty, NetworkParameters params)
      throws Exception {
    BIP47Wallet bip47Wallet = cahootsWallet.getBip47Wallet();
    int bip47Account = cahootsWallet.getBip47Account().getId();
    SegwitAddress receiveAddress =
        bip47Util
            .getReceiveAddress(bip47Wallet, bip47Account, paymentCodeCounterparty, 0, params)
            .getSegwitAddressReceive();
    return receiveAddress;
  }

  private SegwitAddress getMeeetingAddressSend(
      CahootsWallet cahootsWallet, PaymentCode paymentCodeInitiator, NetworkParameters params)
      throws Exception {
    BIP47Wallet bip47Wallet = cahootsWallet.getBip47Wallet();
    int bip47Account = cahootsWallet.getBip47Account().getId();
    SegwitAddress sendAddress =
        bip47Util
            .getSendAddress(bip47Wallet, bip47Account, paymentCodeInitiator, 0, params)
            .getSegwitAddressSend();
    return sendAddress;
  }

  public RpcService getRpcService() {
    return rpcService;
  }
}
