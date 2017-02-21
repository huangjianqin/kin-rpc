package org.kinrpc.rpc.cluster.router;

import org.kinrpc.rpc.invoker.ReferenceInvoker;

import java.util.List;

/**
 * Created by 健勤 on 2017/2/15.
 * 一个默认的实现
 * 不进行任何过滤
 */
public class SimpleRouter implements Router {
    public List<ReferenceInvoker> router(List<ReferenceInvoker> invokers) {
        return invokers;
    }
}
