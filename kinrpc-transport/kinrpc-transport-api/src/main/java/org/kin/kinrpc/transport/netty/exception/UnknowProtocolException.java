package org.kin.kinrpc.transport.netty.exception;

/**
 * @author huangjianqin
 * @date 2019/7/4
 */
public class UnknowProtocolException extends RuntimeException {
    public UnknowProtocolException(int id) {
        super("unknow protocol '" + id + "'");
    }
}
