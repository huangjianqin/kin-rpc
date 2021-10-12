package org.kin.kinrpc.message.core.exception;

/**
 * client stopped异常
 *
 * @author huangjianqin
 * @date 2020-06-19
 */
public final class ClientStoppedException extends RuntimeException {
    public ClientStoppedException(String address) {
        super(String.format("client stopped(right: %s)", address));
    }
}

