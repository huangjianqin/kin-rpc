package org.kin.kinrpc.serialization;

/**
 * Created by huangjianqin on 2019/6/4.
 */
public enum SerializationType {
    /**
     * java自带序列化
     */
    JAVA(1),
    /**
     * kryo序列化
     */
    KRYO(2),
    /**
     * hessian2序列化
     */
    HESSIAN2(3),
    /**
     * json序列化
     */
    JSON(4),
    /**
     * protostuff, 与protobuf性能接近, 但不需要编写的.proto的protobuf工具
     */
    PROTOSTUFF(5),
    /**
     * gson, google优化过的json, 更加适合java开发
     */
    GSON(6),
    /**
     * avro
     */
    AVRO(7),
    ;
    /** 唯一标识 */
    private int code;

    SerializationType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
