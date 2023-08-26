package org.kin.kinrpc.beans;

/**
 * @author huangjianqin
 * @date 2023/8/26
 */
public class ScopeBeanException extends RuntimeException {
    private static final long serialVersionUID = -8952901226060332095L;

    public ScopeBeanException(String message) {
        super(message);
    }

    public ScopeBeanException(String message, Throwable cause) {
        super(message, cause);
    }

    public ScopeBeanException(Throwable cause) {
        super(cause);
    }

    public ScopeBeanException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
