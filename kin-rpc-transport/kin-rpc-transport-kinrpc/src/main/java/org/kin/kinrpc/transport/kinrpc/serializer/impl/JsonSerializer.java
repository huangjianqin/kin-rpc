package org.kin.kinrpc.transport.kinrpc.serializer.impl;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.kin.framework.utils.JSON;
import org.kin.kinrpc.transport.kinrpc.serializer.Serializer;
import org.kin.kinrpc.transport.kinrpc.serializer.SerializerType;

import java.io.IOException;

/**
 * todo rpc请求和rpc响应中包含了object 会存在序列化和反序列化结果不一致问题
 *
 * @author huangjianqin
 * @date 2019/7/29
 */
public class JsonSerializer implements Serializer {
    @Override
    public byte[] serialize(Object target) throws IOException {
        return JSON.writeBytes(target);
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> targetClass) throws IOException {
        return JSON.read(bytes, targetClass);
    }

    @Override
    public int type() {
        return SerializerType.JSON.getCode();
    }

    //------------------------------------------------------------------------------------------------------------------
    //处理json序列化带上类型信息
    static {
        ObjectMapper objectMapper = JSON.PARSER;
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

}
