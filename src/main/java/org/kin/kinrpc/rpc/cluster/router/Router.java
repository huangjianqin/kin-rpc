package org.kin.kinrpc.rpc.cluster.router;

import org.kin.kinrpc.rpc.invoker.impl.ReferenceInvoker;

import java.util.List;

/**
 * Created by 健勤 on 2017/2/15.
 */
public interface Router {
    List<ReferenceInvoker> router(List<ReferenceInvoker> invokers);
}