package com.samourai.soroban.client.rpc;

import com.samourai.http.client.IHttpClient;
import com.samourai.soroban.client.RpcWallet;
import com.samourai.soroban.client.SorobanMessage;
import com.samourai.soroban.client.dialog.Encrypter;
import com.samourai.soroban.client.dialog.PaynymEncrypter;
import com.samourai.soroban.client.dialog.RpcDialog;
import com.samourai.wallet.crypto.CryptoUtil;
import org.bitcoinj.core.NetworkParameters;

public class RpcService {
  private final IHttpClient httpClient;
  private final CryptoUtil cryptoUtil;
  private boolean onion;

  public RpcService(IHttpClient httpClient, CryptoUtil cryptoUtil, boolean onion) {
    this.httpClient = httpClient;
    this.cryptoUtil = cryptoUtil;
    this.onion = onion;
  }

  private RpcClient createRpcClient(RpcWallet rpcWallet, String info) {
    NetworkParameters params = rpcWallet.getParams();
    return new RpcClient(info, httpClient, params, onion);
  }

  public RpcClientEncrypted createRpcClientEncrypted(RpcWallet rpcWallet, String info) {
    RpcClient rpcClient = createRpcClient(rpcWallet, info);
    Encrypter encrypter = getEncrypter(rpcWallet);
    return new RpcClientEncrypted(rpcClient, encrypter, null);
  }

  private Encrypter getEncrypter(RpcWallet rpcWallet) {
    return new PaynymEncrypter(
        rpcWallet.getPaymentCode(),
        rpcWallet.getPaymentCodeKey(),
        rpcWallet.getParams(),
        cryptoUtil);
  }

  public RpcDialog createRpcDialog(RpcWallet rpcWallet, String info, String directory)
      throws Exception {
    RpcClient rpcClient = createRpcClient(rpcWallet, info);
    Encrypter encrypter = getEncrypter(rpcWallet);
    return new RpcDialog(directory, rpcClient, encrypter);
  }

  public RpcDialog createRpcDialog(RpcWallet rpcWallet, String info, SorobanMessage sorobanMessage)
      throws Exception {
    return createRpcDialog(rpcWallet, info, sorobanMessage.toPayload());
  }
}
