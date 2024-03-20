package com.samourai.soroban.client.endpoint.meta.wrapper;

import com.samourai.soroban.client.endpoint.meta.SorobanItem;
import com.samourai.soroban.client.endpoint.meta.SorobanMetadata;
import com.samourai.soroban.client.exception.SorobanException;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.util.Pair;
import java.util.Arrays;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;

/** Metadata: sender */
public class SorobanWrapperMetaSender implements SorobanWrapperMeta {
  private static final String KEY_SENDER = "sender";

  @Override
  public Pair<String, SorobanMetadata> onSend(
      Bip47Encrypter encrypter, Pair<String, SorobanMetadata> entry, Object initialPayload)
      throws Exception {
    // set sender
    String sender = encrypter.getPaymentCode().toString();
    entry.getRight().setMeta(KEY_SENDER, sender);
    return entry;
  }

  @Override
  public Pair<String, SorobanMetadata> onReceive(
      Bip47Encrypter encrypter, Pair<String, SorobanMetadata> entry) throws Exception {
    // require sender
    PaymentCode sender = getSender(entry.getRight());
    if (sender == null) {
      throw new SorobanException("Invalid metadata.sender: " + entry.getRight());
    }
    return entry;
  }

  public static PaymentCode getSender(SorobanMetadata metadata) {
    String sender = metadata.getMetaString(KEY_SENDER);
    if (StringUtils.isEmpty(sender)) {
      return null;
    }
    return new PaymentCode(sender);
  }

  public static <I extends SorobanItem> Stream<I> distinctBySenderWithLastNonce(
      Stream<I> payloads) {
    BinaryOperator<I> mergeLastByNonce =
        (BinaryOperator<I>) SorobanWrapperMetaNonce.mergeLastByNonce();
    return payloads
        .collect(
            Collectors.toMap(
                // distinct by sender
                p -> p.getMetaSender().toString(),
                p -> p,
                // keep last payload with highest nonce
                mergeLastByNonce))
        .values()
        .stream();
  }

  public static <I extends SorobanItem> Predicate<I> filterBySender(PaymentCode... senders) {
    List<String> sendersList =
        Arrays.stream(senders).map(sender -> sender.toString()).collect(Collectors.toList());
    return item -> sendersList.contains(getSender(item.getMetadata()).toString());
  }
}
