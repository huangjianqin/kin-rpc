package org.kin.kinrpc.rpc.exception;

/**
 * @author huangjianqin
 * @date 2019/7/1
 */
public class RpcCallRetryOutException extends RuntimeException {
    private static final long serialVersionUID = -3905072844482013547L;

    public RpcCallRetryOutException(int retryTimes) {
        super("retry rpc call more than " + retryTimes + " times");
    }
}
