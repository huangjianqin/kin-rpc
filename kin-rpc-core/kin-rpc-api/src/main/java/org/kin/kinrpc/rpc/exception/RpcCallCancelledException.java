package org.kin.kinrpc.rpc.exception;

/**
 * @author huangjianqin
 * @date 2020/11/12
 */
public class RpcCallCancelledException extends RuntimeException {
    public RpcCallCancelledException(String errorMsg) {
        super(errorMsg);
    }
}
