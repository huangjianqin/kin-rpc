package org.kin.kinrpc.demo.api;

import org.kin.kinrpc.Interceptor;
import org.kin.kinrpc.Invocation;
import org.kin.kinrpc.Invoker;
import org.kin.kinrpc.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author huangjianqin
 * @date 2023/7/2
 */
public class LogInterceptor implements Interceptor {
    private static final Logger log = LoggerFactory.getLogger(LogInterceptor.class);

    private final boolean serviceOrNot;

    public LogInterceptor(boolean serviceOrNot) {
        this.serviceOrNot = serviceOrNot;
    }

    @Override
    public RpcResult intercept(Invoker<?> invoker, Invocation invocation) {
        log.info((serviceOrNot ? "service" : "reference") +
                " intercept!!! invoker={}, invocation={}", invoker, invocation);
        return invoker.invoke(invocation);
    }
}
