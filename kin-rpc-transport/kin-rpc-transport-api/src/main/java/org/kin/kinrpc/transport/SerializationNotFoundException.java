package org.kin.kinrpc.transport;

/**
 * @author huangjianqin
 * @date 2023/6/7
 */
public class SerializationNotFoundException extends RuntimeException{
    private static final long serialVersionUID = 7149937325634374655L;

    public SerializationNotFoundException() {
    }

    public SerializationNotFoundException(String message) {
        super(message);
    }

    public SerializationNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public SerializationNotFoundException(Throwable cause) {
        super(cause);
    }

    public SerializationNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
