package org.kin.kinrpc.transport;

/**
 * transport相关异常
 *
 * @author huangjianqin
 * @date 2023/6/1
 */
public class TransportException extends RuntimeException {
    private static final long serialVersionUID = -4704870950427891414L;

    public TransportException() {
    }

    public TransportException(String message) {
        super(message);
    }

    public TransportException(String message, Throwable cause) {
        super(message, cause);
    }

    public TransportException(Throwable cause) {
        super(cause);
    }

    public TransportException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
