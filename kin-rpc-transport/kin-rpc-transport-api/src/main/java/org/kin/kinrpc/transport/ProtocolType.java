package org.kin.kinrpc.transport;

/**
 * @author huangjianqin
 * @date 2020/11/16
 */
public enum ProtocolType {
    /**
     * 基于netty tcp的自研发传输层
     */
    KINRPC,
    /**
     * 基于json rpc
     */
    HTTP,
    /**
     * 基于grpc
     */
    GRPC,
    /**
     * 在同一jvm内部直接调用
     */
    JVM,
    /**
     * 基于rsocket
     */
    RSOCKET;

}
