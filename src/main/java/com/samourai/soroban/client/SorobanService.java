package com.samourai.soroban.client;

import com.samourai.soroban.client.dialog.RpcDialog;
import com.samourai.soroban.client.protocol.SorobanProtocolMeeting;
import com.samourai.soroban.client.rpc.RpcClientService;
import com.samourai.soroban.client.rpc.RpcSession;
import com.samourai.wallet.bip47.BIP47UtilGeneric;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.cahoots.CahootsContext;
import com.samourai.wallet.cahoots.CahootsWallet;
import com.samourai.wallet.sorobanClient.SorobanInteraction;
import com.samourai.wallet.sorobanClient.SorobanMessage;
import com.samourai.wallet.sorobanClient.SorobanMessageService;
import com.samourai.wallet.sorobanClient.SorobanReply;
import com.samourai.wallet.util.AsyncUtil;
import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanService {
  private static final Logger log = LoggerFactory.getLogger(SorobanService.class);
  private static final AsyncUtil asyncUtil = AsyncUtil.getInstance();

  private BIP47UtilGeneric bip47Util;
  private NetworkParameters params;
  private RpcClientService rpcClientService;
  private SorobanProtocolMeeting sorobanProtocol;

  public SorobanService(
      BIP47UtilGeneric bip47Util,
      NetworkParameters params,
      RpcClientService rpcClientService,
      SorobanProtocolMeeting sorobanProtocol) {
    this.bip47Util = bip47Util;
    this.params = params;
    this.rpcClientService = rpcClientService;
    this.sorobanProtocol = sorobanProtocol;
  }

  public Observable<SorobanMessage> initiator(
      final CahootsContext cahootsContext,
      RpcSession rpcSession,
      final SorobanMessageService messageService,
      final PaymentCode paymentCodeCounterParty,
      final int timeoutMs,
      final SorobanMessage message,
      Consumer<OnlineSorobanInteraction> onInteraction)
      throws Exception {
    SorobanInteractionHandler interactionHandler = new SorobanInteractionHandler(onInteraction);
    final BehaviorSubject<SorobanMessage> onMessage = BehaviorSubject.create();
    CahootsWallet cahootsWallet = cahootsContext.getCahootsWallet();
    String info = "[initiator]";
    Thread t =
        new Thread(
            () -> {
              RpcDialog dialogOrNull = null;
              try {
                String initialDirectory =
                    sorobanProtocol.getMeeetingAddressSend(
                        cahootsWallet.getBip47Account(),
                        paymentCodeCounterParty,
                        params,
                        bip47Util);
                dialogOrNull = rpcSession.createRpcDialog(initialDirectory);
                final RpcDialog dialog = dialogOrNull;
                closeDialogOnError(onMessage, dialog, paymentCodeCounterParty, interactionHandler);
                dialog(
                    cahootsContext,
                    messageService,
                    dialog,
                    paymentCodeCounterParty,
                    timeoutMs,
                    message,
                    info,
                    onMessage,
                    interactionHandler);
                onMessage.onComplete();
              } catch (Exception e) {
                log.error(info + "=> error", e);
                fail(e.getMessage(), onMessage, dialogOrNull, paymentCodeCounterParty);
              }
            });
    t.setName("soroban-initiator");
    t.start();
    return onMessage;
  }

  public Observable<SorobanMessage> counterparty(
      final CahootsContext cahootsContext,
      RpcSession rpcSession,
      final SorobanMessageService messageService,
      final PaymentCode paymentCodeInitiator,
      final int timeoutMs)
      throws Exception {
    final BehaviorSubject onMessage = BehaviorSubject.create();
    CahootsWallet cahootsWallet = cahootsContext.getCahootsWallet();
    String info = "[counterparty]";
    Thread t =
        new Thread(
            () -> {
              RpcDialog dialogOrNull = null;
              try {
                String initialDirectory =
                    sorobanProtocol.getMeeetingAddressReceive(
                        cahootsWallet.getBip47Account(), paymentCodeInitiator, params, bip47Util);
                dialogOrNull = rpcSession.createRpcDialog(initialDirectory);
                final RpcDialog dialog = dialogOrNull;
                closeDialogOnError(onMessage, dialog, paymentCodeInitiator, null);

                SorobanMessage message =
                    receive(messageService, dialog, paymentCodeInitiator, timeoutMs);
                onMessage.onNext(message);
                if (log.isDebugEnabled()) {
                  log.debug(info + "#(0) <= " + message.toString());
                }
                if (message.isDone()) {
                  if (log.isDebugEnabled()) {
                    log.debug(info + "#(0) done.");
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
                    info,
                    onMessage,
                    null);
                onMessage.onComplete();
              } catch (Exception e) {
                log.error(info + "=> error ", e);
                fail(e.getMessage(), onMessage, dialogOrNull, paymentCodeInitiator);
              }
            });
    t.setName("soroban-counterparty");
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
      dialog.sendError("Cahoots failed: " + e.getMessage(), paymentCodePartner).subscribe();
      log.error("Cahoots failed: " + e.getMessage(), e);
      throw e;
    }
  }

  private SorobanMessage dialog(
      CahootsContext cahootsContext,
      SorobanMessageService messageService,
      RpcDialog dialog,
      PaymentCode paymentCodePartner,
      int timeoutMs,
      SorobanMessage message,
      String info,
      Subject<SorobanMessage> onMessage,
      SorobanInteractionHandler interactionHandler // only set for initiator
      ) throws Exception {
    int i = 0;
    while (true) {
      // send first message
      if (log.isDebugEnabled()) {
        log.debug(info + "#" + i + " => " + message.toString());
      }
      if (onMessage != null) {
        onMessage.onNext(message);
      }
      asyncUtil.blockingAwait(dialog.send(message, paymentCodePartner));

      if (message.isDone()) {
        // done
        if (log.isDebugEnabled()) {
          log.debug(info + "#" + i + " done.");
        }
        break;
      }

      // receive response
      message = receive(messageService, dialog, paymentCodePartner, timeoutMs);
      if (onMessage != null) {
        onMessage.onNext(message);
      }
      if (log.isDebugEnabled()) {
        log.debug(info + "#" + i + " <= " + message.toString());
      }

      if (message.isDone()) {
        // done
        if (log.isDebugEnabled()) {
          log.debug(info + "#" + i + " done.");
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
          log.debug(info + "#" + i + " => [INTERACTIVE] ... >? " + interaction);
        }
        interactionHandler.getOnInteraction().onNext(interaction);

        // wait for interaction confirmation
        try {
          message =
              interactionHandler.getInteractiveMessageProvider().blockingNext().iterator().next();
        } catch (RuntimeException e) {
          throw asyncUtil.unwrapException(e);
        }
        if (log.isDebugEnabled()) {
          log.debug(info + "#" + i + " => [INTERACTIVE] " + message.toString());
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

  private SorobanMessage receive(
      final SorobanMessageService messageService,
      RpcDialog dialog,
      PaymentCode paymentCodePartner,
      int timeoutMs)
      throws Exception {
    String payload = dialog.receive(paymentCodePartner, timeoutMs);
    SorobanMessage response = messageService.parse(payload);
    return response;
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
      // send error before closing dialog
      try {
        asyncUtil.blockingAwait(dialog.sendError(error, paymentCodePartner));
      } catch (Exception e) {
      }
      dialog.close();
    }

    onMessage.onError(new Exception(error));
    onMessage.onComplete();
  }

  public RpcClientService getRpcClientService() {
    return rpcClientService;
  }

  public NetworkParameters getParams() {
    return params;
  }
}
