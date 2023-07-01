package org.kin.kinrpc.demo.api;

/**
 * @author huangjianqin
 * @date 2023/7/2
 */
public class BusinessException extends RuntimeException {
    private static final long serialVersionUID = 142691773205186205L;

    public BusinessException() {
    }

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    public BusinessException(Throwable cause) {
        super(cause);
    }

    public BusinessException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
