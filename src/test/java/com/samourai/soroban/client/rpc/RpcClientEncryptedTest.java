package com.samourai.soroban.client.rpc;

import com.samourai.soroban.client.AbstractTest;
import com.samourai.soroban.client.SorobanPayload;
import com.samourai.soroban.client.meeting.SorobanMessageWithSender;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class RpcClientEncryptedTest extends AbstractTest {
  private static final Logger log = LoggerFactory.getLogger(RpcClientEncryptedTest.class);
  private static final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(5);

  private RpcClient rpcClientInitiator;
  private RpcClientEncrypted rpcClientInitiatorEncrypted;
  private RpcClient rpcClientCounterparty;
  private RpcClientEncrypted rpcClientCounterpartyEncrypted;

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();
    this.rpcClientInitiator = rpcClientService.getRpcClient("initiator");
    this.rpcClientInitiatorEncrypted =
        rpcClientInitiator.createRpcClientEncrypted(cahootsWalletInitiator.getRpcWallet());

    this.rpcClientCounterparty = rpcClientService.getRpcClient("counterparty");
    this.rpcClientCounterpartyEncrypted =
        rpcClientCounterparty.createRpcClientEncrypted(cahootsWalletCounterparty.getRpcWallet());
  }

  @Test
  public void sendEncrypted_receiveEncrypted() throws Exception {
    String key = "key"+ System.currentTimeMillis();
    String value = "valueOne";

    // cleanup
    asyncUtil.blockingGet(rpcClientInitiator.directoryRemove(key, value));
    Assertions.assertTrue(
        asyncUtil.blockingGet(rpcClientInitiatorEncrypted.listWithSender(key)).isEmpty());

    // send
    asyncUtil.blockingAwait(
        rpcClientInitiatorEncrypted.sendEncrypted(key, value, paymentCodeCounterparty));

    // receive
    String result =
        asyncUtil.blockingGet(
            rpcClientCounterpartyEncrypted.receiveEncrypted(key, TIMEOUT_MS, paymentCodeInitiator));
    Assertions.assertEquals(value, result);
  }

  @Test
  public void sendEncryptedWithSender_receiveEncryptedWithSender() throws Exception {
    String key = "key"+System.currentTimeMillis();
    String value = "valueOne";

    // cleanup
    asyncUtil.blockingGet(rpcClientInitiator.directoryRemove(key, value));
    int nbExisting = asyncUtil.blockingGet(rpcClientCounterpartyEncrypted.listWithSender(key)).size();

    // send
    SorobanPayload sorobanPayload =
        new SorobanPayload() {
          @Override
          public String toPayload() {
            return value;
          }
        };
    asyncUtil.blockingAwait(
        rpcClientInitiatorEncrypted.sendEncryptedWithSender(
            key, sorobanPayload, paymentCodeCounterparty));

    // listWithSender
    Collection<SorobanMessageWithSender> results =
        asyncUtil.blockingGet(rpcClientCounterpartyEncrypted.listWithSender(key));
    Assertions.assertEquals(nbExisting+1, results.size());
    SorobanMessageWithSender mws = results.iterator().next();
    Assertions.assertEquals(paymentCodeInitiator.toString(), mws.getSender());
    Assertions.assertEquals(value, mws.getPayload());

    // receive
    mws =
        asyncUtil.blockingGet(
            rpcClientCounterpartyEncrypted.receiveEncryptedWithSender(key, TIMEOUT_MS));
    Assertions.assertEquals(paymentCodeInitiator.toString(), mws.getSender());
    Assertions.assertEquals(value, mws.getPayload());
  }
}
