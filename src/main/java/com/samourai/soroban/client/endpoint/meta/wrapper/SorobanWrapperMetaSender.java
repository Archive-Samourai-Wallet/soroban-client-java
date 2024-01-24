package com.samourai.soroban.client.endpoint.meta.wrapper;

import com.samourai.soroban.client.endpoint.meta.SorobanEntryMeta;
import com.samourai.soroban.client.endpoint.meta.SorobanItem;
import com.samourai.soroban.client.endpoint.meta.SorobanMetadata;
import com.samourai.soroban.client.exception.SorobanException;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;

/** Metadata: sender */
public class SorobanWrapperMetaSender implements SorobanWrapperMeta {
  private static final String KEY_SENDER = "sender";

  @Override
  public SorobanEntryMeta onSend(
      Bip47Encrypter encrypter, SorobanEntryMeta sorobanEntry, Object initialPayload)
      throws Exception {
    // set sender
    String sender = encrypter.getPaymentCode().toString();
    sorobanEntry.getMetadata().setMeta(KEY_SENDER, sender);
    return sorobanEntry;
  }

  @Override
  public SorobanEntryMeta onReceive(Bip47Encrypter encrypter, SorobanEntryMeta sorobanEntry)
      throws Exception {
    // require sender
    PaymentCode sender = getSender(sorobanEntry.getMetadata());
    if (sender == null) {
      throw new SorobanException("Invalid metadata.sender: " + sorobanEntry.getMetadata());
    }
    return sorobanEntry;
  }

  public static PaymentCode getSender(SorobanMetadata metadata) {
    String sender = metadata.getMetaString(KEY_SENDER);
    if (StringUtils.isEmpty(sender)) {
      return null;
    }
    return new PaymentCode(sender);
  }

  public static <I extends SorobanItem> List<I> distinctLatestBySender(Stream<I> payloads) {
    BinaryOperator<I> mergeLastByNonce =
        (BinaryOperator<I>) SorobanWrapperMetaNonce.mergeLastByNonce();
    return new LinkedList<>(
        payloads
            .collect(
                Collectors.toMap(
                    // distinct by sender
                    p -> p.getMetaSender().toString(),
                    p -> p,
                    // keep last payload with highest nonce
                    mergeLastByNonce))
            .values());
  }
}
