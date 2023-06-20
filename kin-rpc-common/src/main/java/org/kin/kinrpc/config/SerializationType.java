package org.kin.kinrpc.config;

/**
 * serialization支持类型
 * @author huangjianqin
 * @date 2023/6/16
 */
public enum SerializationType {
    /** java自带序列化 */
    JAVA("java"),
    /** kryo序列化 */
    KRYO("ktyo"),
    /** hessian2序列化 */
    HESSIAN2("hessian2"),
    /** json序列化 */
    JSON("json"),
    /**
     * protobuf,
     * 如果是protobuf生成的消息则直接调用对应的方法进行序列化和反序列化(已支持从protobuf消息中, 寻找解析构建消息的parseFrom静态方法, 然后使用反射构建消息实例, 不再需要开发者手动注册parser)
     * 否则使用protostuff(与protobuf性能接近, 但不需要编写的.proto的protobuf工具)
     */
    PROTOBUF("protobuf"),
    /** gson, google优化过的json, 更加适合java开发 */
    GSON("gson"),
    /** avro */
    AVRO("avro"),
    /** kinbuffer */
    KIN_BUFFER("kinbuffer"),
    /** jsonb, 将json以bytes形式序列化实现, 有效压缩字节数 */
    JSONB("jsonb"),
    ;
    private final String name;

    SerializationType(String name) {
        this.name = name;
    }

    //getter
    public String getName() {
        return name;
    }
}
