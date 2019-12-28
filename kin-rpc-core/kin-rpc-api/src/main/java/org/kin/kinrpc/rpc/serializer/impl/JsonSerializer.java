package org.kin.kinrpc.rpc.serializer.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.kin.framework.utils.JSON;
import org.kin.kinrpc.rpc.serializer.Serializer;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * @author huangjianqin
 * @date 2019/7/29
 */
public class JsonSerializer implements Serializer {
    @Override
    public byte[] serialize(Object target) throws IOException {
        return JSON.parser.writeValueAsBytes(target);
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> tagetClass) throws IOException {
        return JSON.parser.readValue(bytes, tagetClass);
    }
}
