package org.kin.kinrpc.message.exception;

import org.kin.kinrpc.message.core.OutBoxMessage;

/**
 * client发送消息等待响应超时异常
 *
 * @author huangjianqin
 * @date 2021/10/12
 */
public class AskMessageTimeoutException extends RuntimeException {
    private static final long serialVersionUID = 418969959575017400L;

    public AskMessageTimeoutException(OutBoxMessage message) {
        super(String.format("client waiting response timeout after ask, %s", message));
    }
}