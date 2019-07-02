package org.kin.kinrpc.rpc.exception;

/**
 * @author huangjianqin
 * @date 2019/7/2
 */
public class RPCCallErrorException extends RuntimeException {
    public RPCCallErrorException(String errorMsg) {
        super(errorMsg);
    }
}
