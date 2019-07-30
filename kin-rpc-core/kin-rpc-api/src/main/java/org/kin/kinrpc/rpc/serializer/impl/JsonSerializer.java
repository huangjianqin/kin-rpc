package org.kin.kinrpc.rpc.serializer.impl;

import com.alibaba.fastjson.JSON;
import org.kin.kinrpc.rpc.serializer.Serializer;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * @author huangjianqin
 * @date 2019/7/29
 */
public class JsonSerializer implements Serializer {
    private static final Charset UTF8 = Charset.forName("UTF-8");

    @Override
    public byte[] serialize(Object target) throws IOException {
        return JSON.toJSONString(target).getBytes();
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> tagetClass) throws IOException, ClassNotFoundException {
        String json = new String(bytes, UTF8);
        return JSON.parseObject(json, tagetClass);
    }
}
