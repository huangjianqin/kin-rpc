package org.kin.kinrpc.message.core.exception;

import java.io.Serializable;

/**
 * client发送消息等待响应超时异常
 *
 * @author huangjianqin
 * @date 2021/10/12
 */
public final class RequestResponseTimeoutException extends RuntimeException {
    private static final long serialVersionUID = 418969959575017400L;

    public RequestResponseTimeoutException(Serializable message) {
        super(String.format("client waiting response timeout after request, %s", message));
    }
}