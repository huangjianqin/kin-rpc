package org.kin.kinrpc.rpc.exception;

/**
 * @author huangjianqin
 * @date 2019/7/1
 */
public class RPCRetryOutException extends RuntimeException {
    public RPCRetryOutException(int retryTimes) {
        super("invoke get unvalid response more than " + retryTimes + " times");
    }
}
