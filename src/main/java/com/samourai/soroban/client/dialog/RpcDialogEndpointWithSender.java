package com.samourai.soroban.client.dialog;

import com.samourai.soroban.client.endpoint.AbstractSorobanEndpoint;
import com.samourai.soroban.client.endpoint.SorobanEndpoint;
import com.samourai.soroban.client.endpoint.meta.SorobanFilter;
import com.samourai.soroban.client.endpoint.wrapper.SorobanWrapperString;
import com.samourai.soroban.client.meeting.SorobanMessageWithSender;
import com.samourai.soroban.client.rpc.RpcMode;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.sorobanClient.SorobanPayloadable;
import com.samourai.wallet.util.Pair;

public class RpcDialogEndpointWithSender
    extends AbstractSorobanEndpoint<
        SorobanMessageWithSender,
        SorobanPayloadable,
        PaymentCode,
        SorobanFilter<SorobanMessageWithSender>> {
  public RpcDialogEndpointWithSender(String dir) {
    super(dir, RpcMode.FAST, new SorobanWrapperString[] {});
  }

  @Override
  protected Pair<String, PaymentCode> encryptOnSend(
      Bip47Encrypter encrypter, Pair<String, PaymentCode> entry, Object initialPayload)
      throws Exception {
    return super.encryptOnSend(encrypter, entry, initialPayload);
  }

  @Override
  protected Pair<String, PaymentCode> encryptTo(
      Bip47Encrypter encrypter,
      Pair<String, PaymentCode> entry,
      Object initialPayload,
      PaymentCode encryptTo)
      throws Exception {
    // encrypt payload
    String encryptedPayload =
        super.encryptTo(encrypter, entry, initialPayload, encryptTo).getLeft();

    // wrap with clear sender
    PaymentCode paymentCodeMine = encrypter.getPaymentCode();
    String payload = SorobanMessageWithSender.toPayload(paymentCodeMine, encryptedPayload);
    return Pair.of(payload, entry.getRight());
  }

  @Override
  protected Pair<String, PaymentCode> decryptOnReceive(
      Bip47Encrypter encrypter, Pair<String, PaymentCode> entry) throws Exception {
    // read paymentCode from sender
    SorobanMessageWithSender messageWithSender = SorobanMessageWithSender.parse(entry.getLeft());
    String encryptedPayload = messageWithSender.getPayload();
    PaymentCode sender = messageWithSender.getSender();
    setDecryptFrom(sender); // just for log info

    // decrypt payload
    Pair<String, PaymentCode> e = Pair.of(encryptedPayload, entry.getRight());
    String decryptedPayload = super.decryptFrom(encrypter, e, sender).getLeft();
    return Pair.of(decryptedPayload, sender);
  }

  @Override
  protected SorobanEndpoint newEndpointReply(
      SorobanMessageWithSender request, Bip47Encrypter encrypter) {
    return null;
  }

  @Override
  protected Pair<String, PaymentCode> newEntry(SorobanPayloadable payload) throws Exception {
    return Pair.of(payload.toPayload(), null);
  }

  @Override
  protected String entryToRaw(Pair<String, PaymentCode> entry) throws Exception {
    return entry.getLeft();
  }

  @Override
  protected Pair<String, PaymentCode> rawToEntry(String rawEntry) throws Exception {
    return Pair.of(rawEntry, null);
  }

  @Override
  protected SorobanMessageWithSender newEntry(Pair<String, PaymentCode> entry, String rawEntry) {
    return new SorobanMessageWithSender(entry.getRight(), entry.getLeft(), rawEntry);
  }

  @Override
  protected String getRawEntry(SorobanMessageWithSender entry) {
    return entry.getRawEntry();
  }

  @Override
  protected SorobanFilter<SorobanMessageWithSender> newFilterBuilder() {
    return null; // TODO
  }

  @Override
  public RpcDialogEndpointWithSender setDecryptFrom(PaymentCode decryptFrom) {
    return (RpcDialogEndpointWithSender) super.setDecryptFrom(decryptFrom);
  }

  @Override
  public RpcDialogEndpointWithSender setEncryptTo(PaymentCode encryptTo) {
    return (RpcDialogEndpointWithSender) super.setEncryptTo(encryptTo);
  }
}
