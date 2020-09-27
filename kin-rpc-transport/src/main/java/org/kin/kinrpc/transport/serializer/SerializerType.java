package org.kin.kinrpc.transport.serializer;

/**
 * Created by huangjianqin on 2019/6/4.
 */
public enum SerializerType {
    /**
     * java自带序列化
     */
    JAVA(0),
    /**
     * kryo序列化
     */
    KRYO(1),
    /**
     * hession2序列化
     */
    HESSION2(2),
    /**
     * json序列化
     */
    JSON(3),
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
