package org.kin.kinrpc.rpc.cluster.router.impl;

import org.kin.kinrpc.rpc.cluster.router.Router;
import org.kin.kinrpc.rpc.invoker.AbstractReferenceInvoker;

import java.util.List;

/**
 * Created by 健勤 on 2017/2/15.
 * 一个默认的实现
 * 不进行任何过滤
 */
public class SimpleRouter implements Router {
    @Override
    public List<AbstractReferenceInvoker> router(List<AbstractReferenceInvoker> invokers) {
        return invokers;
    }
}
