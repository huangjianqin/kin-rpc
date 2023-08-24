package org.kin.kinrpc.filter;

import org.kin.kinrpc.*;
import org.kin.kinrpc.constants.InvocationConstants;
import org.kin.kinrpc.constants.Scopes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2023/8/24
 */
@Scope(Scopes.APPLICATION)
public class RpcCallProfileFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(RpcCallProfileFilter.class);

    @Override
    public RpcResult invoke(Invoker<?> invoker, Invocation invocation) {
        try {
            //真正执行服务方法调用的invoker
            Invoker<?> rpcCallInvoker = invocation.attachment(InvocationConstants.RPC_CALL_INVOKER_KEY);
            if (Objects.nonNull(rpcCallInvoker)) {
                //mark invoker active and watch
                int invokerId = rpcCallInvoker.hashCode();
                int handlerId = invocation.handlerId();
                RpcCallProfiler.watch(invokerId, handlerId);

                if (!invocation.hasAttachment(InvocationConstants.RPC_CALL_START_TIME_KEY)) {
                    //attach
                    invocation.attach(InvocationConstants.RPC_CALL_START_TIME_KEY, System.currentTimeMillis());
                }
            }

            return invoker.invoke(invocation);
        } catch (Exception e) {
            log.error("rpc call profile error, invocation={}", invocation, e);
            return invoker.invoke(invocation);
        }
    }

    @Override
    public void onResponse(Invocation invocation, RpcResponse response) {
        Invoker<?> rpcCallInvoker = invocation.attachment(InvocationConstants.RPC_CALL_INVOKER_KEY);
        if (Objects.isNull(rpcCallInvoker)) {
            return;
        }

        int invokerId = rpcCallInvoker.hashCode();
        int handlerId = invocation.handlerId();

        if (!invocation.hasAttachment(InvocationConstants.RPC_CALL_START_TIME_KEY)) {
            return;
        }

        long startTime = invocation.longAttachment(InvocationConstants.RPC_CALL_START_TIME_KEY);
        long endTime = invocation.longAttachment(InvocationConstants.RPC_CALL_END_TIME_KEY, System.currentTimeMillis());
        //remoting发起rpc request耗时
        long elapsed = endTime - startTime;

        //默认true
        boolean succeeded = true;
        Throwable exception = response.getException();
        //是否是provider侧
        boolean provider = rpcCallInvoker instanceof RpcService;
        if (provider) {
            //provider端, 只要是异常, 即说明服务调用异常
            succeeded = Objects.isNull(exception);
        } else {
            //consumer端, 需过滤服务方法执行异常 or 限流流控异常
            if (Objects.nonNull(exception) &&
                    !(exception instanceof ServerErrorException) &&
                    !(exception instanceof RpcExceptionBlockException)) {
                succeeded = false;
            }
        }

        RpcCallProfiler.end(invokerId, handlerId, elapsed, succeeded);
    }

    @Override
    public int order() {
        return HIGHEST_ORDER;
    }
}
