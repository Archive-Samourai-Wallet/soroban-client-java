package examples;

import com.samourai.soroban.client.endpoint.SorobanApp;
import com.samourai.soroban.client.endpoint.meta.typed.SorobanEndpointTyped;
import com.samourai.soroban.client.endpoint.meta.typed.SorobanItemTyped;
import com.samourai.soroban.client.endpoint.meta.typed.SorobanListTyped;
import com.samourai.soroban.client.endpoint.meta.wrapper.*;
import com.samourai.soroban.client.endpoint.wrapper.SorobanWrapper;
import com.samourai.soroban.client.exception.UnexpectedSorobanPayloadTypedException;
import com.samourai.soroban.client.rpc.*;
import com.samourai.wallet.bip47.rpc.BIP47Account;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.util.AsyncUtil;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolNetwork;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;

public class SorobanAppExample {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final AsyncUtil asyncUtil = AsyncUtil.getInstance();
  private static final RpcClientService rpcClientService = null; // TODO provide impl

  // new Soroban app
  private SorobanApp app = new SorobanApp(WhirlpoolNetwork.TESTNET, "EXAMPLE", "1.0");

  // new communication channel
  private SorobanEndpointTyped endpoint = new SorobanEndpointTyped(
          app,
          "DEMO_ENCRYPTED",
          RpcMode.SHORT,
          new SorobanWrapper[] {
                  // optional: enable metadata "sender" for enabling encrypted request replies
                  new SorobanWrapperMetaSender(),
                  // optional: encrypt payload for already-known receiver
                  new SorobanWrapperMetaEncryptWithSender(new PaymentCode("paymentCodeReceiver")),
                  // optional: restrict users
                  new SorobanWrapperMetaFilterSender(new PaymentCode[]{new PaymentCode("foo")}),
                  // optional: restrict payload types
                  new SorobanWrapperMetaFilterType(new Class[]{}),
                  // optional: sign payload (useful for unencrypted messages)
                  new SorobanWrapperMetaSignWithSender(),
                  // optional: use nonce to allow re-pushing the same payload and sorting payloads per time
                  new SorobanWrapperMetaNonce()
          },
          new Class[] {TestPayload.class});

  public void client() throws Exception {
    // create new Soroban client with anonymous identity
    RpcSession rpcSession = rpcClientService.generateRpcWallet().createRpcSession();

    if (false) {
      // or: create new Soroban client authenticated by Paynym
      BIP47Account bip47Account = null; // TODO provide impl
      rpcSession = rpcClientService.getRpcWallet(bip47Account).createRpcSession();
    }

    // use withSorobanClient() to choose a random Soroban node (and retry with next node on network failures)
    // or: use withSorobanClient(, serverUrl) to use a specific Soroban node
    TestPayload request = new TestPayload("HELLO WORLD");
    rpcSession.withSorobanClient(sorobanClient -> {
      // send payload
      return endpoint.send(sorobanClient, request);
    });

    // send payload and wait reply
    SorobanItemTyped reply = asyncUtil.blockingGet(
            endpoint.loopSendUntilReply(rpcSession, request, 2000));

    // send payload and wait specific reply
    TestResponsePayload replyObject = asyncUtil.blockingGet(
            endpoint.loopSendUntilReplyObject(rpcSession, request, 2000, TestResponsePayload.class));

    // send payload and wait ACK
    endpoint.loopSendUntilReplyAck(rpcSession, request, 2000);

    // receive payload
    TestPayload payloadObject = rpcSession.withSorobanClient(sorobanClient -> {
      // get next payload
      SorobanItemTyped payload = asyncUtil.blockingGet(endpoint.getNext(sorobanClient),5000).get();
      log.info("sender: "+payload.getMetaSender());
      log.info("nonce: "+payload.getMetaNonce());
      // handle multiple payload types
      TestPayload testPayload = payload.readOn(TestPayload.class, p -> log.info("This is a TestPayload!"));
      TestResponsePayload testResponsePayload = payload.readOn(TestResponsePayload.class, p -> log.info("This is a TestResponsePayload!"));
      // handle specific payload type: throws UnexpectedSorobanPayloadTypedException when not such type
      TestPayload object = payload.read(TestPayload.class);

      // get all payloads
      SorobanListTyped listPayloads = asyncUtil.blockingGet(endpoint.getList(sorobanClient))
              // sort by time
        .sortByNonce(false)
              // filter by type
              .filterByType(TestPayload.class)
              // keep only the latest by sender
              .distinctLatestBySender();
      List<TestPayload> testPayloads = listPayloads.getListObjects(TestPayload.class);
      return null;
    });

    rpcSession.withSorobanClient(sorobanClient -> {
      // get next payload
      SorobanItemTyped payload = asyncUtil.blockingGet(endpoint.getNext(sorobanClient),5000).get();

      // send reply
      payload.getEndpointReply().send(sorobanClient, new TestResponsePayload("OK"));

      // send ACK
      payload.getEndpointReply().sendAck(sorobanClient);

      return payload.read(TestPayload.class);
    });
  }
}