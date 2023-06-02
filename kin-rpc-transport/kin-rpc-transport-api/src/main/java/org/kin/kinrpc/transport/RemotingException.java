package org.kin.kinrpc.transport;

/**
 * remoting相关异常
 * @author huangjianqin
 * @date 2023/6/2
 */
public class RemotingException extends RuntimeException{
    private static final long serialVersionUID = 8886741713860860844L;

    public RemotingException() {
    }

    public RemotingException(String message) {
        super(message);
    }

    public RemotingException(String message, Throwable cause) {
        super(message, cause);
    }

    public RemotingException(Throwable cause) {
        super(cause);
    }

    public RemotingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
