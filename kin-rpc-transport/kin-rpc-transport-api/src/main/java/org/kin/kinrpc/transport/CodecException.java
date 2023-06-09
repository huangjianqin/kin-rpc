package org.kin.kinrpc.transport;

/**
 * 编解码相关异常
 * @author huangjianqin
 * @date 2023/6/7
 */
public class CodecException extends RuntimeException {
    private static final long serialVersionUID = 7149937325634374655L;

    public CodecException() {
    }

    public CodecException(String message) {
        super(message);
    }

    public CodecException(String message, Throwable cause) {
        super(message, cause);
    }

    public CodecException(Throwable cause) {
        super(cause);
    }

    public CodecException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
