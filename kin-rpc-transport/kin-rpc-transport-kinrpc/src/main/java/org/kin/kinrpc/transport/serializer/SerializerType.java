package org.kin.kinrpc.transport.serializer;

/**
 * Created by huangjianqin on 2019/6/4.
 */
public enum SerializerType {
    /**
     * java自带序列化
     */
    JAVA(1),
    /**
     * kryo序列化
     */
    KRYO(2),
    /**
     * hession2序列化
     */
    HESSION2(3),
    /**
     * json序列化
     */
    @Deprecated
    JSON(4),
    ;
    /** 唯一标识 */
    private int code;

    SerializerType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
