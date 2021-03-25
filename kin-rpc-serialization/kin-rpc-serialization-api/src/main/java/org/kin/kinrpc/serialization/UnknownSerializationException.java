package org.kin.kinrpc.serialization;

/**
 * 未知serialization异常
 *
 * @author huangjianqin
 * @date 2020/9/27
 */
public class UnknownSerializationException extends RuntimeException {
    private static final long serialVersionUID = 7560767097433956518L;

    public UnknownSerializationException(int serializationType) {
        super(String.format("unknown serialization type(code=%s)", serializationType));
    }
}
