package org.kin.kinrpc.rpc.exception;

/**
 * @author huangjianqin
 * @date 2019/6/28
 */
public class RateLimitException extends RuntimeException {
    public RateLimitException() {
        super("rate limited");
    }
}
