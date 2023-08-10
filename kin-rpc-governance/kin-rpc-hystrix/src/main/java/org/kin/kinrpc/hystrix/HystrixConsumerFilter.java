package org.kin.kinrpc.hystrix;

import org.kin.kinrpc.*;
import org.kin.kinrpc.constants.Scopes;

/**
 * @author huangjianqin
 * @date 2023/8/8
 */

@Scope(Scopes.CONSUMER)
public class HystrixConsumerFilter implements Filter {
    @Override
    public RpcResult invoke(Invoker<?> invoker, Invocation invocation) {
        return new KinRpcHystrixCommand(invoker, invocation).invoke();
    }

    @Override
    public int order() {
        return -1000;
    }
}
