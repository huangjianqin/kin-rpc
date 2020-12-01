package org.kin.kinrpc.serializer;

/**
 * 未知serializer异常
 *
 * @author huangjianqin
 * @date 2020/9/27
 */
public class UnknownSerializerException extends RuntimeException {
    private static final long serialVersionUID = 7560767097433956518L;

    public UnknownSerializerException(int serializerType) {
        super(String.format("unknown serializer type(code=%s)", serializerType));
    }
}
