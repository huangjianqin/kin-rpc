package org.kin.kinrpc.transport.serializer.impl;

import org.kin.kinrpc.transport.serializer.Serializer;

import java.io.*;

/**
 *
 * @author 健勤
 * @date 2017/2/9
 */
public class JavaSerializer implements Serializer {
    @Override
    public byte[] serialize(Object target) throws IOException {
        if (target == null) {
            throw new NullPointerException("Serialized object must be not null");
        }

        if (!(target instanceof Serializable)) {
            throw new IllegalStateException("Serialized class " + target.getClass().getSimpleName() + " must implement java.io.Serializable");
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        objectOutputStream.writeObject(target);

        return outputStream.toByteArray();
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> tagetClass) throws IOException, ClassNotFoundException {
        if (bytes == null || bytes.length <= 0) {
            throw new IllegalStateException("byte array must be not null or it's length must be greater than zero");
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
        Object result = objectInputStream.readObject();

        return (T) result;
    }
}