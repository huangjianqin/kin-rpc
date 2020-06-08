package org.kin.kinrpc.rpc.exception;

/**
 * @author huangjianqin
 * @date 2019/7/2
 */
public class RpcCallErrorException extends RuntimeException {
    public RpcCallErrorException(String errorMsg) {
        super(errorMsg);
    }
}
