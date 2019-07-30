package org.kin.kinrpc.config;

/**
 * Created by huangjianqin on 2019/6/4.
 */
public enum SerializerType {
    /**
     * java自带序列化
     */
    JAVA("java"),
    /**
     * kryo序列化
     */
    KRYO("kryo"),
    /**
     * hession序列化
     */
    HESSION("hession"),
    /**
     * json序列化
     */
    JSON("json"),
    ;

    SerializerType(String type) {
        this.type = type;
    }

    private String type;

    public String getType() {
        return type;
    }
}
