package org.kin.kinrpc.demo.api;

import org.kin.kinrpc.Filter;
import org.kin.kinrpc.Invocation;
import org.kin.kinrpc.Invoker;
import org.kin.kinrpc.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author huangjianqin
 * @date 2023/7/2
 */
public class LogFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(LogFilter.class);

    private final boolean serviceOrNot;

    public LogFilter(boolean serviceOrNot) {
        this.serviceOrNot = serviceOrNot;
    }

    @Override
    public RpcResult invoke(Invoker<?> invoker, Invocation invocation) {
        log.info((serviceOrNot ? "service" : "reference") +
                " intercept!!! invoker={}, invocation={}", invoker, invocation);
        return invoker.invoke(invocation);
    }
}
