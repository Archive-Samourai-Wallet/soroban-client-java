package com.samourai.soroban.client.endpoint.meta.wrapper;

import com.samourai.soroban.client.endpoint.meta.SorobanMetadata;
import com.samourai.soroban.client.exception.FilterDeclinedSorobanException;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.util.Pair;
import java.util.Arrays;
import java.util.List;

/** Metadata: sender */
public class SorobanWrapperMetaFilterSender extends SorobanWrapperMetaSender {
  public List<PaymentCode> sendersAllowed;

  public SorobanWrapperMetaFilterSender(PaymentCode... sendersAllowed) {
    this.sendersAllowed = Arrays.asList(sendersAllowed);
  }

  protected boolean isSenderAllowed(PaymentCode sender) {
    return sendersAllowed.contains(sender);
  }

  @Override
  public Pair<String, SorobanMetadata> onSend(
      Bip47Encrypter encrypter, Pair<String, SorobanMetadata> entry, Object initialPayload)
      throws Exception {
    // check sender
    PaymentCode sender = encrypter.getPaymentCode();
    if (!isSenderAllowed(sender)) {
      throw newException(sender);
    }

    // set sender
    return super.onSend(encrypter, entry, initialPayload);
  }

  @Override
  public Pair<String, SorobanMetadata> onReceive(
      Bip47Encrypter encrypter, Pair<String, SorobanMetadata> entry) throws Exception {
    // require sender
    entry = super.onReceive(encrypter, entry);

    // check type
    PaymentCode sender = getSender(entry.getRight());
    if (!isSenderAllowed(sender)) {
      throw newException(sender);
    }
    return entry;
  }

  protected FilterDeclinedSorobanException newException(PaymentCode sender) {
    return new FilterDeclinedSorobanException(
        "Forbidden sender: " + sender + " vs " + Arrays.toString(sendersAllowed.toArray()));
  }
}
