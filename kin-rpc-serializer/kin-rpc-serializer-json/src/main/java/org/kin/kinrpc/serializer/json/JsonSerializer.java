package org.kin.kinrpc.serializer.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.kinrpc.serializer.Serializer;
import org.kin.kinrpc.serializer.SerializerType;

import java.io.IOException;

/**
 * @author huangjianqin
 * @date 2019/7/29
 */
public class JsonSerializer implements Serializer {
    private static final ObjectMapper PARSER = new ObjectMapper();

    static {
        PARSER.findAndRegisterModules();
        //带上类型信息
        //resolved 解决接口参数(返回值)中包含Object类型时, json序列化与反序列化不一致问题, 这样子会增加数据传输的压力, 可通过数据压缩缓解
        PARSER.activateDefaultTypingAsProperty(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.Id.MINIMAL_CLASS.getDefaultPropertyName());
        //允许json中含有指定对象未包含的字段
        PARSER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        //允许序列化空对象
        PARSER.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        //不序列化默认值, 0,false,[],{}等等, 减少json长度
        PARSER.setDefaultPropertyInclusion(JsonInclude.Include.NON_DEFAULT);
        //只认field, 那些get set is开头的方法不生成字段
        PARSER.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        PARSER.setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE);
        PARSER.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
        PARSER.setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE);
    }

    @Override
    public byte[] serialize(Object target) {
        try {
            return PARSER.writeValueAsBytes(target);
        } catch (IOException e) {
            ExceptionUtils.throwExt(e);
        }

        throw new IllegalStateException("encounter unknown error");
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> targetClass) {
        try {
            return PARSER.readValue(bytes, targetClass);
        } catch (IOException e) {
            ExceptionUtils.throwExt(e);
        }

        throw new IllegalStateException("encounter unknown error");
    }

    @Override
    public int type() {
        return SerializerType.JSON.getCode();
    }
}
