package org.kin.kinrpc.serialization;

/**
 * @author huangjianqin
 * @date 2020/11/29
 */
public class SerializationException extends RuntimeException {
    private static final long serialVersionUID = -7916103472234515022L;

    public SerializationException(String message) {
        super(message);
    }

    public SerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
