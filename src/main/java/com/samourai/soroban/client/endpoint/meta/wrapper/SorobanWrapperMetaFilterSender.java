package com.samourai.soroban.client.endpoint.meta.wrapper;

import com.samourai.soroban.client.endpoint.meta.SorobanEntryMeta;
import com.samourai.soroban.client.exception.FilterDeclinedSorobanException;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import com.samourai.wallet.bip47.rpc.PaymentCode;
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
  public SorobanEntryMeta onSend(
      Bip47Encrypter encrypter, SorobanEntryMeta sorobanEntry, Object initialPayload)
      throws Exception {
    // check sender
    PaymentCode sender = encrypter.getPaymentCode();
    if (!isSenderAllowed(sender)) {
      throw newException(sender);
    }

    // set sender
    return super.onSend(encrypter, sorobanEntry, initialPayload);
  }

  @Override
  public SorobanEntryMeta onReceive(Bip47Encrypter encrypter, SorobanEntryMeta sorobanEntry)
      throws Exception {
    // require sender
    sorobanEntry = super.onReceive(encrypter, sorobanEntry);

    // check type
    PaymentCode sender = getSender(sorobanEntry.getMetadata());
    if (!isSenderAllowed(sender)) {
      throw newException(sender);
    }
    return sorobanEntry;
  }

  protected FilterDeclinedSorobanException newException(PaymentCode sender) {
    return new FilterDeclinedSorobanException(
        "Forbidden sender: " + sender + " vs " + Arrays.toString(sendersAllowed.toArray()));
  }
}
