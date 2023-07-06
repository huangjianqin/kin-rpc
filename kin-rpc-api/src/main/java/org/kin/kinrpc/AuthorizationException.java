package org.kin.kinrpc;

/**
 * service token校验异常
 *
 * @author huangjianqin
 * @date 2023/7/6
 */
public class AuthorizationException extends RuntimeException {
    private static final long serialVersionUID = -8533120271628986175L;

    public AuthorizationException() {
    }

    public AuthorizationException(String message) {
        super(message);
    }

    public AuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }

    public AuthorizationException(Throwable cause) {
        super(cause);
    }

    public AuthorizationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
