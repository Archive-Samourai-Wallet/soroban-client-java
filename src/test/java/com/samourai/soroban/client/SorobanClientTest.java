package com.samourai.soroban.client;

import com.samourai.soroban.utils.LogbackUtils;
import org.bitcoinj.core.ECKey;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanClientTest extends AbstractTest {
  private static final Logger log = LoggerFactory.getLogger(SorobanClientTest.class);

  private ECKey signingPrivKey = new ECKey();
  private String signingAddress = signingPrivKey.toAddress(params).toBase58();

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();
    LogbackUtils.setLogLevel("com.samourai.soroban.client.rpc", "TRACE");
  }

  /* TODO
  // SIGNED

  @Test
  public void readSigned_success() throws Exception {
    String key = "readSigned_success";
    TestPayload payload = new TestPayload("HELLO WORLD");

    rpcSessionInitiator.withSorobanClient(
        sorobanClient -> {
          RpcClient rpcClient = sorobanClient.getRpcClient();

          // cleanup
          asyncUtil.blockingAwait(rpcClient.directoryRemoveAll(key));

          // add signed payload
          String signedPayload = sorobanClient.sign(payload.toPayload(), signingPrivKey);
          asyncUtil.blockingAwait(rpcClient.directoryAdd(key, signedPayload, RpcMode.NORMAL));
          return null;
        });

    // get signed payload
    TestPayload resultPayload =
        rpcSessionCounterparty.withSorobanClient(
            sorobanClient -> {
              RpcClient rpcClient = sorobanClient.getRpcClient();
              String signedPayloadStr = asyncUtil.blockingGet(rpcClient.directoryValue(key));
              return sorobanClient
                  .readSigned(signedPayloadStr, signingAddress)
                  .read(TestPayload.class);
            });
    Assertions.assertEquals(payload.getMessage(), resultPayload.getMessage());
  }

  @Test
  public void readSigned_invalid_signature() throws Exception {
    String key = "readSigned_invalid_signature";
    TestPayload payload = new TestPayload("HELLO WORLD");

    rpcSessionInitiator.withSorobanClient(
        sorobanClient -> {
          RpcClient rpcClient = sorobanClient.getRpcClient();
          // cleanup
          asyncUtil.blockingAwait(rpcClient.directoryRemoveAll(key));

          // add signed payload
          String signedPayload = sorobanClient.sign(payload.toPayload(), signingPrivKey);
          asyncUtil.blockingAwait(rpcClient.directoryAdd(key, signedPayload, RpcMode.NORMAL));
          return null;
        });

    // get signed payload
    Exception e =
        Assertions.assertThrows(
            SorobanException.class,
            () -> {
              rpcSessionCounterparty.withSorobanClient(
                  sorobanClient -> {
                    RpcClient rpcClient = sorobanClient.getRpcClient();
                    String signedPayloadStr = asyncUtil.blockingGet(rpcClient.directoryValue(key));
                    String signingAddressInvalid = "tb1q2s8kr83fkxc65q9axmhk0mfmqn6astjsn0fzzd";
                    return sorobanClient
                        .readSigned(signedPayloadStr, signingAddressInvalid)
                        .read(TestPayload.class);
                  });
            });
    Assertions.assertEquals("Invalid signature", e.getMessage());
  }

  @Test
  public void readSigned_errorMessage() throws Exception {
    String key = "readSigned_errorMessage";
    SorobanErrorMessage payload = new SorobanErrorMessage(123, "TEST");

    rpcSessionInitiator.withSorobanClient(
        sorobanClient -> {
          RpcClient rpcClient = sorobanClient.getRpcClient();

          // cleanup
          asyncUtil.blockingAwait(rpcClient.directoryRemoveAll(key));

          // add signed payload
          String signedPayload = sorobanClient.sign(payload.toPayload(), signingPrivKey);
          asyncUtil.blockingAwait(rpcClient.directoryAdd(key, signedPayload, RpcMode.NORMAL));
          return null;
        });

    // get signed payload
    Exception e =
        Assertions.assertThrows(
            SorobanErrorMessageException.class,
            () -> {
              rpcSessionCounterparty.withSorobanClient(
                  sorobanClient -> {
                    RpcClient rpcClient = sorobanClient.getRpcClient();
                    String signedPayloadStr = asyncUtil.blockingGet(rpcClient.directoryValue(key));
                    return sorobanClient
                        .readSigned(signedPayloadStr, signingAddress)
                        .read(TestPayload.class);
                  });
            });
    Assertions.assertEquals("Error 123: TEST", e.getMessage());
  }

  // WITH SENDER

  @Test
  public void readWithSender_success() throws Exception {
    String key = "readWithSender_success";
    TestPayload payload = new TestPayload("HELLO WORLD");

    rpcSessionInitiator.withSorobanClient(
        sorobanClient -> {
          RpcClient rpcClient = sorobanClient.getRpcClient();
          // cleanup
          asyncUtil.blockingAwait(rpcClient.directoryRemoveAll(key));

          // add payload with sender
          String withSenderPayload = sorobanClient.withSender(payload);
          asyncUtil.blockingAwait(rpcClient.directoryAdd(key, withSenderPayload, RpcMode.NORMAL));
          return null;
        });

    // get payload with sender
    PayloadWithSender<TestPayload> payloadAndSender =
        rpcSessionCounterparty.withSorobanClient(
            sorobanClient -> {
              RpcClient rpcClient = sorobanClient.getRpcClient();
              String payloadWithSenderStr = asyncUtil.blockingGet(rpcClient.directoryValue(key));
              return sorobanClient
                  .readWithSender(payloadWithSenderStr)
                  .readWithSender(TestPayload.class);
            });
    Assertions.assertEquals(payload.getMessage(), payloadAndSender.getPayload().getMessage());
    Assertions.assertEquals(
        paymentCodeInitiator.toString(), payloadAndSender.getSender().toString());
  }

  // SIGNED WITH SENDER

  @Test
  public void readSignedWithSender_success() throws Exception {
    String key = "readSignedWithSender_success";
    TestPayload payload = new TestPayload("HELLO WORLD");

    rpcSessionInitiator.withSorobanClient(
        sorobanClient -> {
          RpcClient rpcClient = sorobanClient.getRpcClient();
          // cleanup
          asyncUtil.blockingAwait(rpcClient.directoryRemoveAll(key));

          // add signed payload
          String signedPayload = sorobanClient.signWithSender(payload.toPayload());
          asyncUtil.blockingAwait(rpcClient.directoryAdd(key, signedPayload, RpcMode.NORMAL));
          return null;
        });

    // get signed payload
    PayloadWithSender<TestPayload> payloadAndSender =
        rpcSessionCounterparty.withSorobanClient(
            sorobanClient -> {
              RpcClient rpcClient = sorobanClient.getRpcClient();
              String signedPayloadStr = asyncUtil.blockingGet(rpcClient.directoryValue(key));
              return sorobanClient
                  .readSignedWithSender(signedPayloadStr)
                  .readWithSender(TestPayload.class);
            });
    Assertions.assertEquals(payload.getMessage(), payloadAndSender.getPayload().getMessage());
    Assertions.assertEquals(
        paymentCodeInitiator.toString(), payloadAndSender.getSender().toString());
  }

  @Test
  public void readSignedWithSender_invalid() throws Exception {
    String key = "readSignedWithSender_invalid";
    TestPayload payload = new TestPayload("HELLO WORLD");

    rpcSessionInitiator.withSorobanClient(
        sorobanClient -> {
          RpcClient rpcClient = sorobanClient.getRpcClient();
          // cleanup
          asyncUtil.blockingAwait(rpcClient.directoryRemoveAll(key));

          // add signed payload
          String signedPayload = sorobanClient.signWithSender(payload.toPayload());
          asyncUtil.blockingAwait(rpcClient.directoryAdd(key, signedPayload, RpcMode.NORMAL));
          return null;
        });

    // get signed payload
    Exception e =
        Assertions.assertThrows(
            SorobanException.class,
            () -> {
              rpcSessionCounterparty.withSorobanClient(
                  sorobanClient -> {
                    RpcClient rpcClient = sorobanClient.getRpcClient();
                    String signedPayloadStr = asyncUtil.blockingGet(rpcClient.directoryValue(key));
                    String signedPayloadStrInvalid =
                        signedPayloadStr.replace(
                            paymentCodeInitiator.toString(),
                            "PM8TJMMz8WtG43R88Q2XbUDUSJCyHZrgpcXMQCXpdbuGEsoNSVuXBxqoqHDyy9LXhwwScaixTGLCocjbNZZzJmu91DhHFM7vcXGYNHtWZbj9A54muq3Q");
                    return sorobanClient.readSignedWithSender(signedPayloadStrInvalid);
                  });
            });
    Assertions.assertEquals("Invalid signature", e.getMessage());
  }

  // ENCRYPTED

  @Test
  public void readEncrypted_success() throws Exception {
    String key = "readEncrypted_success";
    TestPayload payload = new TestPayload("HELLO WORLD");

    rpcSessionInitiator.withSorobanClient(
        sorobanClient -> {
          RpcClient rpcClient = sorobanClient.getRpcClient();
          // cleanup
          asyncUtil.blockingAwait(rpcClient.directoryRemoveAll(key));

          // add encrypted payload
          String encryptedPayload =
              sorobanClient.encrypt(payload.toPayload(), paymentCodeCounterparty);
          asyncUtil.blockingAwait(rpcClient.directoryAdd(key, encryptedPayload, RpcMode.NORMAL));
          return null;
        });

    // get encrypted payload
    TestPayload resultPayload =
        rpcSessionCounterparty.withSorobanClient(
            sorobanClient -> {
              RpcClient rpcClient = sorobanClient.getRpcClient();
              String signedPayloadStr = asyncUtil.blockingGet(rpcClient.directoryValue(key));
              return sorobanClient
                  .readEncrypted(signedPayloadStr, paymentCodeInitiator)
                  .read(TestPayload.class);
            });
    Assertions.assertEquals(payload.getMessage(), resultPayload.getMessage());
  }

  @Test
  public void readEncrypted_invalid_encryption() throws Exception {
    String key = "readEncrypted_invalid_encryption";
    TestPayload payload = new TestPayload("HELLO WORLD");

    rpcSessionInitiator.withSorobanClient(
        sorobanClient -> {
          RpcClient rpcClient = sorobanClient.getRpcClient();
          // cleanup
          asyncUtil.blockingAwait(rpcClient.directoryRemoveAll(key));

          // add encrypted payload
          PaymentCode invalidPaymentCode =
              rpcClientService.generateRpcWallet().getBip47Account().getPaymentCode();
          String encryptedPayload = sorobanClient.encrypt(payload.toPayload(), invalidPaymentCode);
          asyncUtil.blockingAwait(rpcClient.directoryAdd(key, encryptedPayload, RpcMode.NORMAL));
          return null;
        });

    // get encrypted payload
    Exception e =
        Assertions.assertThrowsExactly(
            SorobanException.class,
            () -> {
              rpcSessionCounterparty.withSorobanClient(
                  sorobanClient -> {
                    RpcClient rpcClient = sorobanClient.getRpcClient();
                    String signedPayloadStr = asyncUtil.blockingGet(rpcClient.directoryValue(key));
                    return sorobanClient
                        .readEncrypted(signedPayloadStr, paymentCodeInitiator)
                        .read(TestPayload.class);
                  });
            });
    Assertions.assertEquals("Payload decryption failed", e.getMessage());
  }

  @Test
  public void readEncrypted_errorMessage() throws Exception {
    String key = "readEncrypted_errorMessage";
    SorobanErrorMessage payload = new SorobanErrorMessage(123, "TEST");

    rpcSessionInitiator.withSorobanClient(
        sorobanClient -> {
          RpcClient rpcClient = sorobanClient.getRpcClient();
          // cleanup
          asyncUtil.blockingAwait(rpcClient.directoryRemoveAll(key));

          // add encrypted payload
          String encryptedPayload =
              sorobanClient.encrypt(payload.toPayload(), paymentCodeCounterparty);
          asyncUtil.blockingAwait(rpcClient.directoryAdd(key, encryptedPayload, RpcMode.NORMAL));
          return null;
        });

    // get encrypted payload
    Exception e =
        Assertions.assertThrows(
            SorobanErrorMessageException.class,
            () -> {
              rpcSessionCounterparty.withSorobanClient(
                  sorobanClient -> {
                    RpcClient rpcClient = sorobanClient.getRpcClient();
                    String signedPayloadStr = asyncUtil.blockingGet(rpcClient.directoryValue(key));
                    return sorobanClient
                        .readEncrypted(signedPayloadStr, paymentCodeInitiator)
                        .read(TestPayload.class);
                  });
            });
    Assertions.assertEquals("Error 123: TEST", e.getMessage());
  }

  // ENCRYPT_WITH_SENDER

  @Test
  public void encryptWithSender_success() throws Exception {
    String key = "encryptWithSender_success";
    TestPayload payload = new TestPayload("HELLO WORLD");

    rpcSessionInitiator.withSorobanClient(
        sorobanClient -> {
          RpcClient rpcClient = sorobanClient.getRpcClient();
          // cleanup
          asyncUtil.blockingAwait(rpcClient.directoryRemoveAll(key));

          // add encrypted payload
          String encryptedPayload =
              sorobanClient.encryptWithSender(payload.toPayload(), paymentCodeCounterparty);
          asyncUtil.blockingAwait(rpcClient.directoryAdd(key, encryptedPayload, RpcMode.NORMAL));
          return null;
        });

    // get encrypted payload
    PayloadWithSender<TestPayload> result =
        rpcSessionCounterparty.withSorobanClient(
            sorobanClient -> {
              RpcClient rpcClient = sorobanClient.getRpcClient();
              String encryptedPayloadStr = asyncUtil.blockingGet(rpcClient.directoryValue(key));
              return sorobanClient
                  .readEncryptedWithSender(encryptedPayloadStr)
                  .readWithSender(TestPayload.class);
            });
    Assertions.assertEquals(paymentCodeInitiator.toString(), result.getSender().toString());
    Assertions.assertEquals(payload.getMessage(), result.getPayload().getMessage());
  }

  @Test
  public void encryptWithSender_invalid() throws Exception {
    String key = "encryptWithSender_invalid";
    TestPayload payload = new TestPayload("HELLO WORLD");

    rpcSessionInitiator.withSorobanClient(
        sorobanClient -> {
          RpcClient rpcClient = sorobanClient.getRpcClient();
          // cleanup
          asyncUtil.blockingAwait(rpcClient.directoryRemoveAll(key));

          // add encrypted payload
          String encryptedPayload =
              sorobanClient.encryptWithSender(payload.toPayload(), paymentCodeCounterparty);
          asyncUtil.blockingAwait(rpcClient.directoryAdd(key, encryptedPayload, RpcMode.NORMAL));
          return null;
        });

    // get encrypted payload
    Exception e =
        Assertions.assertThrows(
            SorobanException.class,
            () -> {
              rpcSessionCounterparty.withSorobanClient(
                  sorobanClient -> {
                    RpcClient rpcClient = sorobanClient.getRpcClient();
                    String encryptedPayloadStr =
                        asyncUtil.blockingGet(rpcClient.directoryValue(key));
                    String encryptedPayloadStrInvalid =
                        encryptedPayloadStr.replace(
                            paymentCodeInitiator.toString(),
                            "PM8TJMMz8WtG43R88Q2XbUDUSJCyHZrgpcXMQCXpdbuGEsoNSVuXBxqoqHDyy9LXhwwScaixTGLCocjbNZZzJmu91DhHFM7vcXGYNHtWZbj9A54muq3Q");
                    return sorobanClient.readEncryptedWithSender(encryptedPayloadStrInvalid);
                  });
            });
    Assertions.assertEquals("Payload decryption failed", e.getMessage());
  }

  @Test
  public void encryptWithSender_errorMessage() throws Exception {
    String key = "encryptWithSender_errorMessage";
    SorobanErrorMessage payload = new SorobanErrorMessage(123, "TEST");

    rpcSessionInitiator.withSorobanClient(
        sorobanClient -> {
          RpcClient rpcClient = sorobanClient.getRpcClient();
          // cleanup
          asyncUtil.blockingAwait(rpcClient.directoryRemoveAll(key));

          // add encrypted payload
          String encryptedPayload =
              sorobanClient.encryptWithSender(payload.toPayload(), paymentCodeCounterparty);
          asyncUtil.blockingAwait(rpcClient.directoryAdd(key, encryptedPayload, RpcMode.NORMAL));
          return null;
        });

    // get encrypted payload
    Exception e =
        Assertions.assertThrows(
            SorobanErrorMessageException.class,
            () -> {
              rpcSessionCounterparty.withSorobanClient(
                  sorobanClient -> {
                    RpcClient rpcClient = sorobanClient.getRpcClient();
                    String encryptedPayloadStr =
                        asyncUtil.blockingGet(rpcClient.directoryValue(key));
                    return sorobanClient
                        .readEncryptedWithSender(encryptedPayloadStr)
                        .readWithSender(TestPayload.class);
                  });
            });
    Assertions.assertEquals("Error 123: TEST", e.getMessage());
  }

  // LIST_ENCRYPTED_WITH_SENDER

  @Test
  public void listEncryptedWithSender_success() throws Exception {
    String key = "listEncryptedWithSender_success";
    TestPayload payload1 = new TestPayload("PAYLOAD1");
    TestPayload payload2 = new TestPayload("PAYLOAD2");
    TestPayload payload3 = new TestPayload("PAYLOAD3"); // invalid encryption
    AnotherTestPayload payload4 = new AnotherTestPayload("PAYLOAD3"); // invalid type

    rpcSessionInitiator.withSorobanClient(
        sorobanClient -> {
          RpcClient rpcClient = sorobanClient.getRpcClient();
          // cleanup
          asyncUtil.blockingAwait(rpcClient.directoryRemoveAll(key));

          // add encrypted payloads
          String encryptedPayload1 =
              sorobanClient.encryptWithSender(payload1.toPayload(), paymentCodeCounterparty);
          asyncUtil.blockingAwait(rpcClient.directoryAdd(key, encryptedPayload1, RpcMode.SHORT));

          String encryptedPayload2 =
              sorobanClient.encryptWithSender(payload2.toPayload(), paymentCodeCounterparty);
          asyncUtil.blockingAwait(rpcClient.directoryAdd(key, encryptedPayload2, RpcMode.SHORT));

          // invalid encryption
          String encryptedPayload3 =
              sorobanClient.encryptWithSender(payload3.toPayload(), paymentCodeCounterparty);
          encryptedPayload3 = encryptedPayload3.replace("payload\":\"", "payload\":\"invalid");
          asyncUtil.blockingAwait(rpcClient.directoryAdd(key, encryptedPayload3, RpcMode.SHORT));

          // invalid type
          String encryptedPayload4 =
              sorobanClient.encryptWithSender(payload4.toPayload(), paymentCodeCounterparty);
          asyncUtil.blockingAwait(rpcClient.directoryAdd(key, encryptedPayload4, RpcMode.SHORT));

          // invalid json
          asyncUtil.blockingAwait(
              rpcClient.directoryAdd(key, "{invalid JSON: \"foo}", RpcMode.SHORT));
          return null;
        });

    // list encrypted payloads
    List<PayloadWithSender<TestPayload>> results =
        asyncUtil.blockingGet(
            rpcSessionCounterparty.withSorobanClient(
                sorobanClient ->
                    sorobanClient
                        .listEncryptedWithSender(key)
                        .map(listUntyped -> listUntyped.readListWithSender(TestPayload.class))));
    Assertions.assertEquals(2, results.size());

    PayloadWithSender<TestPayload> item = results.get(0);
    Assertions.assertEquals(paymentCodeInitiator.toString(), item.getSender().toString());
    Assertions.assertEquals(payload1.getMessage(), item.getPayload().getMessage());

    item = results.get(1);
    Assertions.assertEquals(paymentCodeInitiator.toString(), item.getSender().toString());
    Assertions.assertEquals(payload2.getMessage(), item.getPayload().getMessage());
  }

  @Test
  public void listEncryptedWithSender_success_filtered() throws Exception {
    String key = "listEncryptedWithSender_success_filtered";
    TestPayload payload1 = new TestPayload("PAYLOAD1");
    TestPayload payload2 = new TestPayload("PAYLOAD2");

    rpcSessionInitiator.withSorobanClient(
        sorobanClient -> {
          RpcClient rpcClient = sorobanClient.getRpcClient();
          // cleanup
          asyncUtil.blockingAwait(rpcClient.directoryRemoveAll(key));

          // add encrypted payloads
          String encryptedPayload1 =
              sorobanClient.encryptWithSender(payload1.toPayload(), paymentCodeCounterparty);
          asyncUtil.blockingAwait(rpcClient.directoryAdd(key, encryptedPayload1, RpcMode.SHORT));

          String encryptedPayload2 =
              sorobanClient.encryptWithSender(payload2.toPayload(), paymentCodeCounterparty);
          asyncUtil.blockingAwait(rpcClient.directoryAdd(key, encryptedPayload2, RpcMode.SHORT));
          return null;
        });

    // filter by message
    Predicate<PayloadWithSender<TestPayload>> filter =
        p -> p.getPayload().getMessage().equals(payload1.getMessage());
    // list encrypted payloads
    List<PayloadWithSender<TestPayload>> resultsFiltered =
        asyncUtil.blockingGet(
            rpcSessionCounterparty.withSorobanClient(
                sorobanClient ->
                    sorobanClient
                        .listEncryptedWithSender(key)
                        .map(
                            listUntyped ->
                                listUntyped.readListWithSender(TestPayload.class, filter))));
    Assertions.assertEquals(1, resultsFiltered.size());
    PayloadWithSender<TestPayload> item = resultsFiltered.get(0);
    Assertions.assertEquals(paymentCodeInitiator.toString(), item.getSender().toString());
    Assertions.assertEquals(payload1.getMessage(), item.getPayload().getMessage());
  }

  // LIST_SIGNED_WITH_SENDER

  @Test
  public void listSignedWithSender_success() throws Exception {
    String key = "listSignedWithSender_success";
    TestPayload payload1 = new TestPayload("PAYLOAD1");
    TestPayload payload2 = new TestPayload("PAYLOAD2");
    TestPayload payload3 = new TestPayload("PAYLOAD3"); // invalid

    rpcSessionInitiator.withSorobanClient(
        sorobanClient -> {
          RpcClient rpcClient = sorobanClient.getRpcClient();
          // cleanup
          asyncUtil.blockingAwait(rpcClient.directoryRemoveAll(key));

          // add signed payloads
          String signedPayload1 = sorobanClient.signWithSender(payload1.toPayload());
          asyncUtil.blockingAwait(rpcClient.directoryAdd(key, signedPayload1, RpcMode.SHORT));

          String signedPayload2 = sorobanClient.signWithSender(payload2.toPayload());
          asyncUtil.blockingAwait(rpcClient.directoryAdd(key, signedPayload2, RpcMode.SHORT));

          // invalid signature
          String signedPayload3 = sorobanClient.signWithSender(payload3.toPayload());
          signedPayload3 = signedPayload3.replace("signature\":\"", "signature\":\"invalid");
          asyncUtil.blockingAwait(rpcClient.directoryAdd(key, signedPayload3, RpcMode.SHORT));

          // invalid json
          asyncUtil.blockingAwait(
              rpcClient.directoryAdd(key, "{invalid JSON: \"foo}", RpcMode.SHORT));
          return null;
        });

    // list encrypted payloads
    List<PayloadWithSender<TestPayload>> results =
        asyncUtil.blockingGet(
            rpcSessionCounterparty.withSorobanClient(
                sorobanClient ->
                    sorobanClient
                        .listSignedWithSender(key)
                        .map(untypedList -> untypedList.readListWithSender(TestPayload.class))));
    Assertions.assertEquals(2, results.size());

    PayloadWithSender<TestPayload> item = results.get(0);
    Assertions.assertEquals(paymentCodeInitiator.toString(), item.getSender().toString());
    Assertions.assertEquals(payload1.getMessage(), item.getPayload().getMessage());

    item = results.get(1);
    Assertions.assertEquals(paymentCodeInitiator.toString(), item.getSender().toString());
    Assertions.assertEquals(payload2.getMessage(), item.getPayload().getMessage());
  }

  // LIST_SIGNED_WITH_SENDER_DUPLICATES

  @Test
  public void list_distinctBySender() throws Exception {
    String key = "list_distinctBySender";
    List<TestPayload> payloadsInitiator =
        Arrays.asList(
            new TestPayload("initiator_1"),
            new TestPayload("initiator_2"),
            new TestPayload("initiator_3"));
    List<TestPayload> payloadsCounterparty =
        Arrays.asList(
            new TestPayload("counterparty_1"),
            new TestPayload("counterparty_2"),
            new TestPayload("counterparty_3"));

    // add initiator payloads
    rpcSessionInitiator.withSorobanClient(
        sorobanClient -> {
          RpcClient rpcClient = sorobanClient.getRpcClient();
          // cleanup
          asyncUtil.blockingAwait(rpcClient.directoryRemoveAll(key));

          for (TestPayload payload : payloadsInitiator) {
            // add payload with sender
            String payloadWithSender = sorobanClient.withSender(payload);
            asyncUtil.blockingAwait(rpcClient.directoryAdd(key, payloadWithSender, RpcMode.SHORT));
            if (log.isDebugEnabled()) {
              log.debug(payload.getMessage() + ": " + payload.getTimePayload());
            }
          }
          return null;
        });

    // add counterparty payloads
    rpcSessionCounterparty.withSorobanClient(
        sorobanClient -> {
          RpcClient rpcClient = sorobanClient.getRpcClient();

          for (TestPayload payload : payloadsCounterparty) {
            // add payload with sender
            String payloadWithSender = sorobanClient.withSender(payload);
            asyncUtil.blockingAwait(rpcClient.directoryAdd(key, payloadWithSender, RpcMode.SHORT));
            if (log.isDebugEnabled()) {
              log.debug(payload.getMessage() + ": " + payload.getTimePayload());
            }
          }
          return null;
        });

    // list payloads
    ListPayloadTypedWithSender listUntyped =
        asyncUtil.blockingGet(
            rpcSessionCounterparty.withSorobanClient(
                sorobanClient -> sorobanClient.listWithSender(key)));
    Assertions.assertEquals(6, listUntyped.size());

    listUntyped.distinctBySender();

    Assertions.assertEquals(2, listUntyped.size());
    List<PayloadWithSender<TestPayload>> results =
        listUntyped.readListWithSender(TestPayload.class);

    // should most recent payload from each sender
    PayloadWithSender<TestPayload> item = results.get(0);
    Assertions.assertEquals(paymentCodeInitiator.toString(), item.getSender().toString());
    Assertions.assertEquals("initiator_3", item.getPayload().getMessage());
    Assertions.assertEquals(
        payloadsInitiator.get(2).getTimePayload(), item.getPayload().getTimePayload());

    item = results.get(1);
    Assertions.assertEquals(paymentCodeCounterparty.toString(), item.getSender().toString());
    Assertions.assertEquals("counterparty_3", item.getPayload().getMessage());
    Assertions.assertEquals(
        payloadsCounterparty.get(2).getTimePayload(), item.getPayload().getTimePayload());
  }*/
}
