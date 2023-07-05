package org.kin.kinrpc.protocol;

import org.kin.kinrpc.RpcException;

/**
 * rpc服务方法执行异常, 即server端返回的异常
 *
 * @author huangjianqin
 * @date 2023/6/29
 */
public class ServerErrorException extends RpcException {
    private static final long serialVersionUID = 2615995268526205598L;

    public ServerErrorException(String message) {
        super(message);
    }

    public ServerErrorException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServerErrorException(Throwable cause) {
        super(cause);
    }
}
