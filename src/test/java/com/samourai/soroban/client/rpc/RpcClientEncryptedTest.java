package com.samourai.soroban.client.rpc;

import com.samourai.soroban.client.AbstractTest;
import com.samourai.soroban.client.SorobanPayload;
import com.samourai.soroban.client.meeting.SorobanMessageWithSender;
import java.util.Collection;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcClientEncryptedTest extends AbstractTest {
  private static final Logger log = LoggerFactory.getLogger(RpcClientEncryptedTest.class);
  private static final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(5);

  private RpcSession rpcClientInitiator;
  private RpcClientEncrypted rpcClientInitiatorEncrypted;
  private RpcSession rpcClientCounterparty;
  private RpcClientEncrypted rpcClientCounterpartyEncrypted;

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();
    this.rpcClientInitiator = rpcClientService.getRpcSession("initiator");
    this.rpcClientInitiatorEncrypted =
        rpcClientInitiator.withRpcClientEncrypted(
            cahootsWalletInitiator.getRpcWallet().getEncrypter(), rce -> rce);

    this.rpcClientCounterparty = rpcClientService.getRpcSession("counterparty");
    this.rpcClientCounterpartyEncrypted =
        rpcClientCounterparty.withRpcClientEncrypted(
            cahootsWalletCounterparty.getRpcWallet().getEncrypter(), rce -> rce);
  }

  @Test
  public void sendEncrypted_receiveEncrypted() throws Exception {
    String key = "key" + System.currentTimeMillis();
    String value = "valueOne";

    // cleanup
    asyncUtil.blockingAwait(rpcClientInitiator.withRpcClient(rc -> rc).directoryRemove(key, value));
    Assertions.assertTrue(
        asyncUtil.blockingGet(rpcClientInitiatorEncrypted.listWithSender(key)).isEmpty());

    // send
    asyncUtil.blockingGet(
        rpcClientInitiatorEncrypted.sendEncrypted(
            key, value, paymentCodeCounterparty, RpcMode.NORMAL));

    // receive
    String result =
        asyncUtil.blockingGet(
            rpcClientCounterpartyEncrypted.receiveEncrypted(
                key, TIMEOUT_MS, paymentCodeInitiator, RETRY_DELAY_MS));
    Assertions.assertEquals(value, result);
  }

  @Test
  public void sendEncryptedWithSender_receiveEncryptedWithSender() throws Exception {
    String key = "key" + System.currentTimeMillis();
    String value = "valueOne";

    // cleanup
    asyncUtil.blockingAwait(rpcClientInitiator.withRpcClient(rc -> rc).directoryRemove(key, value));
    int nbExisting =
        asyncUtil.blockingGet(rpcClientCounterpartyEncrypted.listWithSender(key)).size();

    // send
    SorobanPayload sorobanPayload =
        new SorobanPayload() {
          @Override
          public String toPayload() {
            return value;
          }
        };
    asyncUtil.blockingGet(
        rpcClientInitiatorEncrypted.sendEncryptedWithSender(
            key, sorobanPayload, paymentCodeCounterparty, RpcMode.NORMAL));

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
            rpcClientCounterpartyEncrypted.receiveEncryptedWithSender(
                key, TIMEOUT_MS, RETRY_DELAY_MS));
    Assertions.assertEquals(paymentCodeInitiator.toString(), mws.getSender());
    Assertions.assertEquals(value, mws.getPayload());
  }
}
