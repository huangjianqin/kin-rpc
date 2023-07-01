package org.kin.kinrpc.protocol;

import org.kin.kinrpc.RpcException;

/**
 * rpc服务方法调用异常, 即server端返回的异常
 * todo 命名
 *
 * @author huangjianqin
 * @date 2023/6/29
 */
public class RpcBizException extends RpcException {
    private static final long serialVersionUID = 2615995268526205598L;

    public RpcBizException(String message) {
        super(message);
    }

    public RpcBizException(String message, Throwable cause) {
        super(message, cause);
    }

    public RpcBizException(Throwable cause) {
        super(cause);
    }
}
