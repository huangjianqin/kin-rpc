package org.kin.kinrpc.rpc.exception;

/**
 * @author huangjianqin
 * @date 2019/7/1
 */
public class RpcRetryOutException extends RuntimeException {
    public RpcRetryOutException(int retryTimes) {
        super("invoke get unvalid response more than " + retryTimes + " times");
    }
}
