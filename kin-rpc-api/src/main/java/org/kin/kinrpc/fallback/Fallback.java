package org.kin.kinrpc.fallback;

import org.kin.kinrpc.Invocation;
import org.kin.kinrpc.RpcExceptionBlockException;
import org.kin.kinrpc.RpcResult;

/**
 * 服务降级failback
 *
 * @author huangjianqin
 * @date 2023/8/2
 */
public interface Fallback {
    /**
     * 服务降级后, rpc调用处理
     *
     * @param invocation rpc call info
     * @param e          服务降级异常
     * @return 服务降级处理后的rpc call result
     */
    RpcResult handle(Invocation invocation, RpcExceptionBlockException e);
}
