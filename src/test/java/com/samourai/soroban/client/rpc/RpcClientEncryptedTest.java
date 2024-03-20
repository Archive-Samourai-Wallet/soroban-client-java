package com.samourai.soroban.client.rpc;

import com.samourai.soroban.client.AbstractTest;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcClientEncryptedTest extends AbstractTest {
  private static final Logger log = LoggerFactory.getLogger(RpcClientEncryptedTest.class);

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  // TODO move to RpcDialogTest

  /*@Test
  public void sendEncrypted_receiveEncrypted() throws Exception {
    String key = "key" + System.currentTimeMillis();
    String value = "valueOne";

    // cleanup
    asyncUtil.blockingAwait(
        rpcSessionInitiator.withRpcClient(rc -> rc).directoryRemove(key, value));
    Assertions.assertTrue(
        asyncUtil.blockingGet(rpcClientInitiatorEncrypted.listWithSender(key)).isEmpty());

    // send
    asyncUtil.blockingAwait(
        rpcClientInitiatorEncrypted.sendEncrypted(
            key, value, paymentCodeCounterparty, RpcMode.NORMAL));

    // receive
    String result =
        asyncUtil.blockingGet(
            rpcClientCounterpartyEncrypted.receiveEncrypted(key, TIMEOUT_MS, paymentCodeInitiator));
    Assertions.assertEquals(value, result);
  }

  @Test
  public void sendEncryptedWithSender_receiveEncryptedWithSender() throws Exception {
    String key = "key" + System.currentTimeMillis();
    String value = "valueOne";

    // cleanup
    asyncUtil.blockingAwait(
        rpcSessionInitiator.withRpcClient(rc -> rc).directoryRemove(key, value));
    int nbExisting =
        asyncUtil.blockingGet(rpcClientCounterpartyEncrypted.listWithSender(key)).size();

    // send
    asyncUtil.blockingAwait(
        rpcClientInitiatorEncrypted.sendEncryptedWithSender(
            key, value, paymentCodeCounterparty, RpcMode.NORMAL));

    // listWithSender
    Collection<SorobanMessageWithSender> results =
        asyncUtil.blockingGet(rpcClientCounterpartyEncrypted.listWithSender(key));
    Assertions.assertEquals(nbExisting + 1, results.size());
    SorobanMessageWithSender mws = results.iterator().next();
    Assertions.assertEquals(paymentCodeInitiator.toString(), mws.getSender());
    Assertions.assertEquals(value, mws.getPayload());

    // receive
    mws =
        asyncUtil.blockingGet(
            rpcClientCounterpartyEncrypted.receiveEncryptedWithSender(key, TIMEOUT_MS));
    Assertions.assertEquals(paymentCodeInitiator.toString(), mws.getSender());
    Assertions.assertEquals(value, mws.getPayload());
  }*/
}
