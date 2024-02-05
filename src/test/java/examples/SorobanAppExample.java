package examples;

import com.samourai.soroban.client.endpoint.SorobanApp;
import com.samourai.soroban.client.endpoint.meta.typed.SorobanEndpointTyped;
import com.samourai.soroban.client.endpoint.meta.typed.SorobanItemTyped;
import com.samourai.soroban.client.endpoint.meta.wrapper.*;
import com.samourai.soroban.client.endpoint.wrapper.SorobanWrapper;
import com.samourai.soroban.client.rpc.*;
import com.samourai.wallet.bip47.rpc.BIP47Account;
import com.samourai.wallet.bip47.rpc.Bip47Encrypter;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.util.AsyncUtil;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolNetwork;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanAppExample {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final AsyncUtil asyncUtil = AsyncUtil.getInstance();
  private static final RpcClientService rpcClientService = null; // TODO provide impl

  // new Soroban app
  private SorobanApp app = new SorobanApp(WhirlpoolNetwork.TESTNET, "EXAMPLE", "1.0");

  // new communication channel
  private SorobanEndpointTyped endpoint =
      new SorobanEndpointTyped(
          app,
          "DEMO_ENCRYPTED",
          RpcMode.SHORT,
          new SorobanWrapper[] {
            // optional: enable metadata "sender" for enabling encrypted request replies
            new SorobanWrapperMetaSender(),
            // optional: encrypt payload for already-known receiver
            //// TODO new SorobanWrapperMetaEncryptWithSender(new
            // PaymentCode("paymentCodeReceiver")),
            // optional: restrict users
            new SorobanWrapperMetaFilterSender(new PaymentCode[] {new PaymentCode("foo")}),
            // optional: sign payload (to authenticate unencrypted messages)
            new SorobanWrapperMetaSignWithSender(),
            // optional: use nonce to allow re-pushing the same payload and sorting payloads per
            // time
            new SorobanWrapperMetaNonce()
          },
          // optional: restrict payload types (null to allow any types)
          new Class[] {TestPayload.class});

  public void client() throws Exception {
    // create new Soroban client with anonymous identity
    RpcSession rpcSession = rpcClientService.generateRpcWallet().createRpcSession();

    if (false) {
      // or: create new Soroban client authenticated by Paynym
      BIP47Account bip47Account = null; // TODO provide impl
      rpcSession = rpcClientService.getRpcWallet(bip47Account).createRpcSession();
    }

    // use withSorobanClient() to choose a random Soroban node (and retry with next node on network
    // failures)
    // or: use withSorobanClient(, serverUrl) to use a specific Soroban node
    TestPayload request = new TestPayload("HELLO WORLD");
    rpcSession.withSorobanClient(
        sorobanClient -> {
          // send payload
          return endpoint.send(sorobanClient, request);
        });

    // send payload and wait reply for 2sec
    SorobanItemTyped reply =
        endpoint.loopSendUntil(
            rpcSession, request, 2000, query -> endpoint.loopWaitReply(rpcSession, query));

    // send payload and wait reply as object
    TestResponsePayload replyObject =
        endpoint.loopSendUntil(
            rpcSession,
            request,
            2000,
            query -> endpoint.waitReplyObject(rpcSession, query, TestResponsePayload.class));

    // send payload and wait ACK
    endpoint.loopSendUntil(
        rpcSession, request, 2000, query -> endpoint.waitReplyAck(rpcSession, query));

    // receive payload
    TestPayload payloadObject =
        rpcSession.withSorobanClient(
            sorobanClient -> {
              // get any payload
              SorobanItemTyped payload =
                  asyncUtil.blockingGet(endpoint.findAny(sorobanClient), 5000).get();
              log.info("sender: " + payload.getMetaSender());
              log.info("nonce: " + payload.getMetaNonce());

              // do stuff depending on payload type
              Optional<TestPayload> testPayload =
                  payload.readOn(TestPayload.class, p -> log.info("This is a TestPayload!"));
              Optional<TestResponsePayload> testResponsePayload =
                  payload.readOn(
                      TestResponsePayload.class, p -> log.info("This is a TestResponsePayload!"));

              // force to given type
              // throws UnexpectedSorobanPayloadTypedException when not such type
              TestPayload object = payload.read(TestPayload.class);

              // get all payloads
              List<SorobanItemTyped> listPayloads =
                  asyncUtil.blockingGet(
                      endpoint.getList(
                          sorobanClient,
                          f ->
                              // sort by time
                              f.sortByNonce(false)
                                  // filter by type
                                  .filterByType(TestPayload.class)
                                  // keep only the latest by sender
                                  .distinctBySenderWithLastNonce()));

              // get all payloads as objects
              List<TestPayload> listObjects =
                  asyncUtil.blockingGet(
                      endpoint.getListObjects(
                          sorobanClient,
                          TestPayload.class,
                          f ->
                              // sort by time
                              f.sortByNonce(false)
                                  // filter by type
                                  .filterByType(TestPayload.class)
                                  // keep only the latest by sender
                                  .distinctBySenderWithLastNonce()));
              return null;
            });

    rpcSession.withSorobanClient(
        sorobanClient -> {
          // get next payload
          SorobanItemTyped payload =
              asyncUtil.blockingGet(endpoint.findAny(sorobanClient), 5000).get();

          // send reply
          Bip47Encrypter encrypter = rpcSession.getRpcWallet().getBip47Encrypter();
          payload.getEndpointReply(encrypter).send(sorobanClient, new TestResponsePayload("OK"));

          // send ACK
          payload.getEndpointReply(encrypter).sendAck(sorobanClient);

          return payload.read(TestPayload.class);
        });
  }
}
