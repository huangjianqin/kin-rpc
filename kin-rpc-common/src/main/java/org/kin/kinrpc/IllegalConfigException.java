package org.kin.kinrpc;

/**
 * 非法配置异常
 *
 * @author huangjianqin
 * @date 2023/6/30
 */
public class IllegalConfigException extends RuntimeException {
    private static final long serialVersionUID = -621070802435012172L;

    public IllegalConfigException() {
    }

    public IllegalConfigException(String message) {
        super(message);
    }

    public IllegalConfigException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalConfigException(Throwable cause) {
        super(cause);
    }

    public IllegalConfigException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
