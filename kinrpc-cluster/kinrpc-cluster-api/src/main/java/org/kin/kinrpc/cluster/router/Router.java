package org.kin.kinrpc.cluster.router;


import org.kin.kinrpc.rpc.invoker.AbstractReferenceInvoker;

import java.util.List;

/**
 * Created by 健勤 on 2017/2/15.
 */
public interface Router {
    List<AbstractReferenceInvoker> router(List<AbstractReferenceInvoker> invokers);
}
