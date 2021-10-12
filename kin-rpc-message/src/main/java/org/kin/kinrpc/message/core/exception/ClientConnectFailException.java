package org.kin.kinrpc.message.core.exception;

/**
 * client连接异常
 *
 * @author huangjianqin
 * @date 2020-06-19
 */
public final class ClientConnectFailException extends RuntimeException {
    public ClientConnectFailException(String address) {
        super(String.format("client fail(right: %s)", address));
    }
}
