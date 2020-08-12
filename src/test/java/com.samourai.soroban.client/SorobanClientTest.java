package com.samourai.soroban.client;

import com.samourai.soroban.client.rpc.RpcClient;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanClientTest {
  private static final Logger log = LoggerFactory.getLogger(SorobanClientTest.class);

  @Test
  public void pingPong() throws Exception {
    final String torProxy = "127.0.0.1";
    final int torPort = 9050;
    final String torUrl = "http://sorob4sg7yiopktgz4eom7hl5mcodr6quvhmdpljl5qqhmt6po7oebid.onion";
    final String directoryName = "samourai.soroban.private";
    final int numIter = 2;

    // run initiator
    Thread threadInitiator =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                RpcClient rpc = new RpcClient(torProxy, torPort, torUrl);
                try {
                  SorobanPingPong.initiator(rpc, directoryName, numIter);
                } catch (Exception e) {
                  log.error("", e);
                } finally {
                  try {
                    rpc.close();
                  } catch (Exception e) {
                  }
                }
              }
            });
    threadInitiator.start();

    // run contributor
    Thread threadContributor =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                RpcClient rpc = new RpcClient(torProxy, torPort, torUrl);
                try {
                  SorobanPingPong.contributor(rpc, directoryName, numIter);
                } catch (Exception e) {
                  log.error("", e);
                } finally {
                  try {
                    rpc.close();
                  } catch (Exception e) {
                  }
                }
              }
            });
    threadContributor.start();

    synchronized (threadInitiator) {
      threadInitiator.wait();
    }
    log.info("threadInitiator ended");
  }
}
