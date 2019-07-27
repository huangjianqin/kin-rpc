package org.kin.kinrpc.rpc.serializer.impl;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.kin.kinrpc.rpc.serializer.Serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by huangjianqin on 2019/5/29.
 */
public class KryoSerializer implements Serializer {
    private static final Kryo KRYO = new Kryo();

    @Override
    public byte[] serialize(Object target) throws IOException {
        Output output = new Output(new ByteArrayOutputStream());
        KRYO.writeObject(output, target);
        byte[] bytes = output.toBytes();
        output.close();

        return bytes;
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> tagetClass) throws IOException, ClassNotFoundException {
        Input input = new Input(new ByteArrayInputStream(bytes));
        Object request = KRYO.readObject(input, tagetClass);
        input.close();

        return (T) request;
    }
}
