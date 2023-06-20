package org.kin.kinrpc.config;

/**
 * @author huangjianqin
 * @date 2020/11/16
 */
public enum ProtocolType {
    /** 基于netty tcp的自研发传输层 */
    KINRPC("kinrpc"),
    /** 基于grpc */
    GRPC("grpc"),
    /** 在同一jvm内部直接调用 */
    JVM("jvm"),
    /** 基于rsocket */
    RSOCKET("rsocket");

    private final String name;

    ProtocolType(String name) {
        this.name = name;
    }

    //getter
    public String getName() {
        return name;
    }
}
