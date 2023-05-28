package org.kin.kinrpc.rpc;

/**
 * rpc相关异常封装
 * @author huangjianqin
 * @date 2023/2/26
 */
public class RpcException extends RuntimeException{
    private static final long serialVersionUID = -986710218583787816L;

    public RpcException(String message) {
        super(message);
    }

    public RpcException(String message, Throwable cause) {
        super(message, cause);
    }

    public RpcException(Throwable cause) {
        super(cause);
    }
}
