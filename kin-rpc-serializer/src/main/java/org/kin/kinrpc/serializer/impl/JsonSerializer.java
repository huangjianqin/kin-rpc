package org.kin.kinrpc.serializer.impl;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.kin.framework.utils.JSON;
import org.kin.kinrpc.serializer.Serializer;
import org.kin.kinrpc.serializer.SerializerType;

/**
 * @author huangjianqin
 * @date 2019/7/29
 */
public class JsonSerializer implements Serializer {
    @Override
    public byte[] serialize(Object target) {
        return JSON.writeBytes(target);
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> targetClass) {
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
