package org.kin.kinrpc.serializer.impl;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoPool;
import org.kin.kinrpc.serializer.Serializer;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Created by huangjianqin on 2019/5/29.
 */
public class KryoSerializer implements Serializer {
    //解决多线程访问问题
    //1.池化,
    //2.ThreadLocal
    private static final KryoPool pool = new KryoPool.Builder(() -> {
        final Kryo kryo = new Kryo();
        kryo.setInstantiatorStrategy(new Kryo.DefaultInstantiatorStrategy(
                new StdInstantiatorStrategy()));
        return kryo;
    }).softReferences().build();

    @Override
    public byte[] serialize(Object target) {
        Kryo kryo = pool.borrow();
        Output output = new Output(new ByteArrayOutputStream());
        kryo.writeObject(output, target);
        byte[] bytes = output.toBytes();
        output.close();
        pool.release(kryo);

        return bytes;
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> tagetClass) {
        Input input = new Input(new ByteArrayInputStream(bytes));
        Kryo kryo = pool.borrow();
        T request = kryo.readObject(input, tagetClass);
        input.close();
        pool.release(kryo);

        return request;
    }
}
