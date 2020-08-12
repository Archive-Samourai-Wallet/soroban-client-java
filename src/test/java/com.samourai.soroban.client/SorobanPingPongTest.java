package com.samourai.soroban.client;

import com.samourai.soroban.client.rpc.RpcClient;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.RSAPublicKeySpec;
import org.bitcoinj.core.ECKey;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorobanPingPongTest {
  private static final Logger log = LoggerFactory.getLogger(SorobanPingPongTest.class);
  private static final SecureRandom secureRandom = new SecureRandom();

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
                  SorobanPingPong.initiator(rpc, directoryName, new ECKey(), numIter);
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
                  SorobanPingPong.contributor(rpc, directoryName, new ECKey(), numIter);
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

  private byte[] generatePrivateKey() throws Exception {
    return computePrivateKey(generateKeyPair()).getEncoded();
  }

  public AsymmetricCipherKeyPair generateKeyPair() {
    // Generate a 2048-bit RSA key pair.
    RSAKeyPairGenerator generator = new RSAKeyPairGenerator();
    /*new RsaKeyGenerationParameters(
    RSA_F4,
    secureRandom,
    2048,
    100)
    */
    generator.init(
        new RSAKeyGenerationParameters(new BigInteger("10001", 16), secureRandom, 2048, 80));
    return generator.generateKeyPair();
  }

  public PublicKey computePublicKey(AsymmetricCipherKeyPair keyPair) throws Exception {
    RSAKeyParameters pubKey = (RSAKeyParameters) keyPair.getPublic();
    RSAPublicKeySpec rsaPublicKeySpec =
        new RSAPublicKeySpec(pubKey.getModulus(), pubKey.getExponent());
    KeyFactory kf = KeyFactory.getInstance("RSA");
    return kf.generatePublic(rsaPublicKeySpec);
  }

  public PrivateKey computePrivateKey(AsymmetricCipherKeyPair keyPair) throws Exception {
    RSAKeyParameters pubKey = (RSAKeyParameters) keyPair.getPublic();
    RSAPublicKeySpec rsaPublicKeySpec =
        new RSAPublicKeySpec(pubKey.getModulus(), pubKey.getExponent());
    KeyFactory kf = KeyFactory.getInstance("RSA");
    return kf.generatePrivate(rsaPublicKeySpec);
  }
}
