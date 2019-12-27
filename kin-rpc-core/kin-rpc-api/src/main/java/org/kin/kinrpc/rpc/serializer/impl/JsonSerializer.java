package org.kin.kinrpc.rpc.serializer.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.kin.kinrpc.rpc.serializer.Serializer;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * @author huangjianqin
 * @date 2019/7/29
 */
public class JsonSerializer implements Serializer {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public byte[] serialize(Object target) throws IOException {
        return mapper.writeValueAsBytes(target);
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> tagetClass) throws IOException {
        return mapper.readValue(bytes, tagetClass);
    }
}
