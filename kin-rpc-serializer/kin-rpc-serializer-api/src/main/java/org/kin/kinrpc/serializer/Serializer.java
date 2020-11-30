package org.kin.kinrpc.serializer;


import org.kin.framework.utils.SPI;

import java.io.IOException;

/**
 * todo 某些序列化框架支持注册class优化性能, 后续可以考虑使用通用接口优化序列化性能
 * Created by 健勤 on 2017/2/10.
 */
@SPI(value = "kryo", key = "serializer")
public interface Serializer {
    /**
     * 序列化
     *
     * @param target 实例
     * @return 序列化后的字节数组
     * @throws IOException IO异常
     */
    byte[] serialize(Object target) throws IOException;

    /**
     * 反序列化
     *
     * @param bytes       字节数组
     * @param targetClass 指定类
     * @param <T>         指定类
     * @return 反序列化结果
     * @throws IOException            IO异常
     * @throws ClassNotFoundException 找不到class异常
     */
    <T> T deserialize(byte[] bytes, Class<T> targetClass) throws IOException, ClassNotFoundException;

    /**
     * @return 序列化类型code, 必须>0
     */
    int type();
}
