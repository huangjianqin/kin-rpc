package org.kin.kinrpc.rpc.exception;

/**
 * @author huangjianqin
 * @date 2019/7/2
 */
public class UnknownRPCResponseStateCodeException extends RuntimeException {
    public UnknownRPCResponseStateCodeException(int code) {
        super("unknown rpc response state code '" + code + "'");
    }
}
