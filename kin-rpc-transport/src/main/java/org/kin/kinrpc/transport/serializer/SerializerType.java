package org.kin.kinrpc.transport.serializer;

/**
 * Created by huangjianqin on 2019/6/4.
 */
public enum SerializerType {
    /**
     * java自带序列化
     */
    JAVA,
    /**
     * kryo序列化
     */
    KRYO,
    /**
     * hession序列化
     */
    HESSION,
    /**
     * json序列化
     */
    JSON,
    ;

    public String getType() {
        return name().toLowerCase();
    }
}
