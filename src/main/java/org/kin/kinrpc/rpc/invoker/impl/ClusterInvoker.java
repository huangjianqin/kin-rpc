package org.kin.kinrpc.rpc.invoker.impl;

import org.kin.kinrpc.rpc.cluster.Cluster;
import org.kin.kinrpc.rpc.future.RPCFuture;
import org.kin.kinrpc.rpc.invoker.AsyncInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Created by 健勤 on 2017/2/15.
 */
public class ClusterInvoker implements InvocationHandler, AsyncInvoker {
    private static final Logger log = LoggerFactory.getLogger(ClusterInvoker.class);

    private Cluster cluster;

    public ClusterInvoker(Cluster cluster) {
        this.cluster = cluster;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        log.info("invoke method '" + method.getName() + "'");
        return invoke(method.getName(), args);
    }

    public Object invoke(String methodName, Object... params) {
        ReferenceInvoker invoker = cluster.get();
        if (invoker != null) {
            return invoker.invoke(methodName, params);
        } else {
            //抛弃异常
            throw new RuntimeException();
        }
    }

    public RPCFuture invokerAsync(String methodName, Object... params) {
        ReferenceInvoker invoker = cluster.get();
        if (invoker != null) {
            return invoker.invokerAsync(methodName, params);
        } else {
            //抛弃异常
            throw new RuntimeException();
        }
    }

    public Cluster getCluster() {
        return cluster;
    }
}
