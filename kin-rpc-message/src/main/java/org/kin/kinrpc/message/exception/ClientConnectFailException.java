package org.kin.kinrpc.message.exception;

/**
 * @author huangjianqin
 * @date 2020-06-19
 * client连接异常
 */
public class ClientConnectFailException extends RuntimeException {
    public ClientConnectFailException(String address) {
        super(String.format("client fail(right: %s)", address));
    }
}
