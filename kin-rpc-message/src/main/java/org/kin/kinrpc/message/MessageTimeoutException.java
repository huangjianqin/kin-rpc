package org.kin.kinrpc.message;

import org.kin.kinrpc.transport.TransportException;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2023/7/12
 */
public class MessageTimeoutException extends TransportException {
    private static final long serialVersionUID = -7114489192077982841L;

    public MessageTimeoutException(Serializable message) {
        super(String.format("send message and wait response timeout, %s", message));
    }
}
