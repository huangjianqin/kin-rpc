package org.kin.kinrpc.rpc.future;


import org.kin.kinrpc.rpc.transport.domain.RPCRequest;
import org.kin.kinrpc.rpc.transport.domain.RPCResponse;

/**
 * Created by 健勤 on 2017/2/15.
 */
public interface AsyncRPCCallback {
    /**
     * rpc请求成功回调
     *
     * @param rpcResponse rpc返回结果
     */
    void success(RPCResponse rpcResponse);

    /**
     * rpc请求重试回调
     * @param rpcRequest rpc请求
     */
    void retry(RPCRequest rpcRequest);

    /**
     * rpc请求失败回调
     * @param e 异常
     */
    void fail(Exception e);
}
