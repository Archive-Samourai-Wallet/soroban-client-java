package com.samourai.soroban.client;

import com.samourai.soroban.client.exception.SorobanException;
import com.samourai.soroban.client.rpc.RpcClient;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.util.CallbackWithArg;
import com.samourai.wallet.util.MessageSignUtilGeneric;
import com.samourai.wallet.util.Z85;
import io.reactivex.Single;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanClient {
  private static final Logger log = LoggerFactory.getLogger(SorobanClient.class.getName());
  private static final Z85 z85 = Z85.getInstance();
  private static final MessageSignUtilGeneric messageSignUtil =
      MessageSignUtilGeneric.getInstance();

  private final RpcClient rpcClient;
  private Bip47Encrypter encrypter;

  public SorobanClient(RpcClient rpcClient, Bip47Encrypter encrypter) {
    this.rpcClient = rpcClient;
    this.encrypter = encrypter;
  }

  public UntypedPayload read(String payload) {
    return new UntypedPayload(payload);
  }

  public UntypedPayload readEncrypted(String payload, PaymentCode paymentCodePartner)
      throws Exception {
    String decryptedPayload = decrypt(payload, paymentCodePartner);
    return read(decryptedPayload);
  }

  public UntypedPayload readSigned(String payload, String signingAddress) throws Exception {
    // unserialize
    SorobanPayloadSigned sorobanPayloadSigned = read(payload).read(SorobanPayloadSigned.class);

    // verify signature
    String result = verifySignature(sorobanPayloadSigned, signingAddress);
    return read(result);
  }

  protected UntypedPayloadWithSender readSignedWithSender(String payload) throws Exception {
    // unserialize
    SorobanPayloadSignedWithSender sorobanPayloadSigned =
        read(payload).read(SorobanPayloadSignedWithSender.class);

    // verify signature
    PaymentCode signingPaymentCode = new PaymentCode(sorobanPayloadSigned.getSender());
    NetworkParameters params = getParams();
    String signingAddress = signingPaymentCode.notificationAddress(params).getAddressString();
    String signedPayload = verifySignature(sorobanPayloadSigned, signingAddress);
    return new UntypedPayloadWithSender(signedPayload, signingPaymentCode);
  }

  protected <T extends SorobanPayload> String withSender(T payload) throws Exception {
    PaymentCode sender = encrypter.getPaymentCode();
    return new SorobanPayloadWithSender(payload.toPayload().getBytes("UTF-8"), sender.toString())
        .toPayload();
  }

  protected UntypedPayloadWithSender readWithSender(String payloadWithSender) throws Exception {
    // unserialize
    SorobanPayloadWithSender sorobanPayloadWithSender =
        read(payloadWithSender).read(SorobanPayloadWithSender.class);
    String payload = new String(sorobanPayloadWithSender.getPayload(), "UTF-8");
    PaymentCode sender = new PaymentCode(sorobanPayloadWithSender.getSender());
    return new UntypedPayloadWithSender(payload, sender);
  }

  protected byte[] encryptBytes(String payload, PaymentCode paymentCodePartner) throws Exception {
    return encrypter.encrypt(payload, paymentCodePartner);
  }

  protected String decryptBytes(byte[] payload, PaymentCode paymentCodePartner) throws Exception {
    try {
      return encrypter.decrypt(payload, paymentCodePartner);
    } catch (RuntimeException e) {
      log.error("Payload decryption failed", e);
      throw new SorobanException("Payload decryption failed");
    }
  }

  public String encrypt(String payload, PaymentCode paymentCodePartner) throws Exception {
    return z85.encode(encryptBytes(payload, paymentCodePartner));
  }

  public String decrypt(String payload, PaymentCode paymentCodePartner) throws Exception {
    return decryptBytes(z85.decode(payload), paymentCodePartner);
  }

  public String encryptWithSender(String payload, PaymentCode paymentCodePartner) throws Exception {
    byte[] encryptedPayload = encryptBytes(payload, paymentCodePartner);
    return new SorobanPayloadWithSender(encryptedPayload, encrypter.getPaymentCode().toString())
        .toPayload();
  }

  protected UntypedPayloadWithSender readEncryptedWithSender(String payload) throws Exception {
    SorobanPayloadWithSender payloadWithSender = read(payload).read(SorobanPayloadWithSender.class);
    PaymentCode paymentCodeSender = new PaymentCode(payloadWithSender.getSender());
    String decryptedPayload = decryptBytes(payloadWithSender.getPayload(), paymentCodeSender);
    return new UntypedPayloadWithSender(decryptedPayload, paymentCodeSender);
  }

  public String sign(String payload, ECKey signingPrivKey) throws Exception {
    String signature = messageSignUtil.signMessage(signingPrivKey, payload);
    return new SorobanPayloadSigned(payload, signature).toPayload();
  }

  public String signWithSender(String payload) throws Exception {
    String signature = encrypter.sign(payload);
    return new SorobanPayloadSignedWithSender(
            payload, signature, encrypter.getPaymentCode().toString())
        .toPayload();
  }

  protected String verifySignature(SorobanPayloadSigned sorobanPayloadSigned, String signingAddress)
      throws Exception {
    // verify signature
    String signedPayload = sorobanPayloadSigned.getPayload();
    String signature = sorobanPayloadSigned.getSignature();
    NetworkParameters params = getParams();
    boolean valid =
        messageSignUtil.verifySignedMessage(signingAddress, signedPayload, signature, params);
    if (!valid) {
      log.error("Invalid signature: " + signature);
      throw new SorobanException("Invalid signature");
    }
    return signedPayload;
  }

  // LIST

  protected Single<String[]> listValues(String directory) throws Exception {
    return rpcClient.directoryValues(directory);
  }

  public <T> Single<ListUntypedPayloadWithSender> listEncryptedWithSender(String directory)
      throws Exception {
    return listEncryptedWithSender(directory, null);
  }

  public Single<ListUntypedPayloadWithSender> listEncryptedWithSender(
      String directory, Predicate<UntypedPayloadWithSender> filterOrNull) throws Exception {
    return listValues(directory)
        .map(
            values -> {
              CallbackWithArg<String, UntypedPayloadWithSender> adapt =
                  payload -> readEncryptedWithSender(payload);
              Collection<UntypedPayloadWithSender> results =
                  ListUntypedPayload.adaptList(Arrays.asList(values), adapt, filterOrNull);
              return new ListUntypedPayloadWithSender(results);
            });
  }

  public <T> Single<ListUntypedPayloadWithSender> listSignedWithSender(String directory)
      throws Exception {
    return listSignedWithSender(directory, null);
  }

  public Single<ListUntypedPayloadWithSender> listSignedWithSender(
      String directory, Predicate<UntypedPayloadWithSender> filterOrNull) throws Exception {
    return listValues(directory)
        .map(
            values -> {
              CallbackWithArg<String, UntypedPayloadWithSender> adapt =
                  payload -> readSignedWithSender(payload);
              Collection<UntypedPayloadWithSender> results =
                  ListUntypedPayload.adaptList(Arrays.asList(values), adapt, filterOrNull);
              return new ListUntypedPayloadWithSender(results);
            });
  }

  public Single<ListUntypedPayload> list(String directory) throws Exception {
    return list(directory, null);
  }

  public Single<ListUntypedPayload> list(String directory, Predicate<UntypedPayload> filterOrNull)
      throws Exception {
    return listValues(directory)
        .map(
            values -> {
              CallbackWithArg<String, UntypedPayload> adapt = payload -> read(payload);
              Collection<UntypedPayload> results =
                  ListUntypedPayload.adaptList(Arrays.asList(values), adapt, filterOrNull);
              return new ListUntypedPayload(results);
            });
  }

  public Single<ListUntypedPayloadWithSender> listWithSender(String directory) throws Exception {
    return listWithSender(directory, null);
  }

  public Single<ListUntypedPayloadWithSender> listWithSender(
      String directory, Predicate<UntypedPayloadWithSender> filterOrNull) throws Exception {
    return listValues(directory)
        .map(
            values -> {
              CallbackWithArg<String, UntypedPayloadWithSender> adapt =
                  payload -> readWithSender(payload);
              Collection<UntypedPayloadWithSender> results =
                  ListUntypedPayload.adaptList(Arrays.asList(values), adapt, filterOrNull);
              return new ListUntypedPayloadWithSender(results);
            });
  }

  //

  public RpcClient getRpcClient() {
    return rpcClient;
  }

  public NetworkParameters getParams() {
    return rpcClient.getParams();
  }
}
