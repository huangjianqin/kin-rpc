package org.kin.kinrpc.transport.kinrpc.serializer.impl;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import org.kin.kinrpc.transport.kinrpc.serializer.Serializer;
import org.kin.kinrpc.transport.kinrpc.serializer.SerializerType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;

/**
 * Created by 健勤 on 2017/2/9.
 */
public class Hessian2Serializer implements Serializer {
    @Override
    public byte[] serialize(Object target) throws IOException {
        if (target == null) {
            throw new NullPointerException("Serialized object must be not null");
        }

        if (!(target instanceof Serializable)) {
            throw new IllegalStateException("Serialized class " + target.getClass().getSimpleName() + " must implement java.io.Serializable");
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Hessian2Output hessian2Output = new Hessian2Output(outputStream);
        hessian2Output.writeObject(target);

        hessian2Output.close();
        outputStream.close();

        return outputStream.toByteArray();
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> targetClass) throws IOException {
        if (bytes == null || bytes.length <= 0) {
            throw new IllegalStateException("byte array must be not null or it's length must be greater than zero");
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        Hessian2Input hessian2Input = new Hessian2Input(inputStream);
        Object request = hessian2Input.readObject(targetClass);

        hessian2Input.close();
        inputStream.close();

        return (T) request;
    }

    @Override
    public int type() {
        return SerializerType.HESSION2.getCode();
    }
}
