package org.kin.kinrpc.rpc;


/**
 * Created by 健勤 on 2017/2/15.
 */
public interface AsyncRpcCallback {
    /**
     * rpc请求成功回调
     *
     * @param rpcRequest  rpc请求
     * @param rpcResponse rpc返回结果
     */
    void success(RpcRequest rpcRequest, RpcResponse rpcResponse);

    /**
     * rpc请求失败回调
     *
     * @param rpcRequest rpc请求
     * @param e          异常
     */
    void fail(RpcRequest rpcRequest, Exception e);
}
