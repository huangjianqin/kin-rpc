package org.kin.kinrpc.transport;

/**
 * @author huangjianqin
 * @date 2020/11/16
 */
public enum ProtocolType {
    /**
     * 基于netty tcp的自研发传输层
     */
    KinRpc,
    /**
     * 基于json rpc
     */
    Http;
}
