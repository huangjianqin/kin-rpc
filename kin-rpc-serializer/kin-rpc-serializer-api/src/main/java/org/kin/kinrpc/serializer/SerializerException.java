package org.kin.kinrpc.serializer;

/**
 * @author huangjianqin
 * @date 2020/11/29
 */
public class SerializerException extends RuntimeException {
    private static final long serialVersionUID = -7916103472234515022L;

    public SerializerException(String message) {
        super(message);
    }

    public SerializerException(String message, Throwable cause) {
        super(message, cause);
    }
}
