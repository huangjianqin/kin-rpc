package org.kin.kinrpc.rpc.exception;

/**
 * @author huangjianqin
 * @date 2019/6/28
 */
public class RateLimitException extends RuntimeException {
    private static final long serialVersionUID = 186944003099539195L;

    public RateLimitException(String message) {
        super("rate limited >>>".concat(message));
    }
}
