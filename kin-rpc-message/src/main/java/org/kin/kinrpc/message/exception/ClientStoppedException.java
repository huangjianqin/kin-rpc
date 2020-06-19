package org.kin.kinrpc.message.exception;

/**
 * @author huangjianqin
 * @date 2020-06-19
 * <p>
 * client stopped异常
 */
public class ClientStoppedException extends RuntimeException {
    public ClientStoppedException(String address) {
        super(String.format("client stopped(right: %s)", address));
    }
}

