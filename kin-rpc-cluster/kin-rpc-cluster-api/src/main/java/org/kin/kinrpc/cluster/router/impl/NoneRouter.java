package org.kin.kinrpc.cluster.router.impl;

import org.kin.kinrpc.cluster.router.Router;
import org.kin.kinrpc.rpc.AsyncInvoker;

import java.util.List;

/**
 * Created by 健勤 on 2017/2/15.
 * 一个默认的实现
 * 不进行任何过滤
 */
public class NoneRouter implements Router {
    @Override
    public List<AsyncInvoker> router(List<AsyncInvoker> invokers) {
        return invokers;
    }
}
