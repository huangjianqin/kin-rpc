package org.kin.kinrpc.transport.serializer;

/**
 * @author huangjianqin
 * @date 2020/9/27
 */
public class UnknownSerializerException extends RuntimeException {
    public UnknownSerializerException(int serializerType) {
        super(String.format("unknown serializer type(code=%s)", serializerType));
    }
}
