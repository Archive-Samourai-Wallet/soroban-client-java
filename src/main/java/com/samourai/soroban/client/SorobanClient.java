package com.samourai.soroban.client;

import com.samourai.soroban.client.rpc.RpcClient;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import com.samourai.wallet.util.CallbackWithArg;
import io.reactivex.Single;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanClient {
  private static final Logger log = LoggerFactory.getLogger(SorobanClient.class.getName());

  private final RpcClient rpcClient;
  private Bip47Encrypter encrypter;

  public SorobanClient(RpcClient rpcClient, Bip47Encrypter encrypter) {
    this.rpcClient = rpcClient;
    this.encrypter = encrypter;
  }

  public SorobanPayloadTyped readTyped(String payload) throws Exception {
    return SorobanPayloadTyped.parse(payload);
  }

  protected SorobanPayloadTyped readWithSender(String payloadWithSender) throws Exception {
    // unserialize
    return readTyped(payloadWithSender).read(SorobanPayloadTyped.class);
  }

  // LIST

  protected Single<String[]> listValues(String directory) throws Exception {
    return rpcClient.directoryValues(directory);
  }

  public Single<ListPayloadTyped> list(String directory) throws Exception {
    return list(directory, null);
  }

  public Single<ListPayloadTyped> list(
      String directory, Predicate<SorobanPayloadTyped> filterOrNull) throws Exception {
    return listValues(directory)
        .map(
            values -> {
              CallbackWithArg<String, SorobanPayloadTyped> adapt = payload -> readTyped(payload);
              Collection<SorobanPayloadTyped> results =
                  ListPayloadTyped.adaptList(Arrays.asList(values), adapt, filterOrNull);
              return new ListPayloadTyped(results);
            });
  }

  //

  public RpcClient getRpcClient() {
    return rpcClient;
  }

  public Bip47Encrypter getEncrypter() {
    return encrypter;
  }

  public NetworkParameters getParams() {
    return rpcClient.getParams();
  }
}
