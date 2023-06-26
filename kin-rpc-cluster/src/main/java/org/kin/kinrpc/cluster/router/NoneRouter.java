package org.kin.kinrpc.cluster.router;

import org.kin.kinrpc.ReferenceInvoker;

import java.util.List;

/**
 * 不进行任何过滤
 * Created by 健勤 on 2017/2/15.
 */
public class NoneRouter implements Router {
    @Override
    public List<ReferenceInvoker<?>> route(List<ReferenceInvoker<?>> invokers) {
        return invokers;
    }
}
