package org.kin.kinrpc.cluster.router;


import org.kin.kinrpc.rpc.common.Spi;
import org.kin.kinrpc.rpc.invoker.impl.ReferenceInvoker;

import java.util.List;

/**
 * Created by 健勤 on 2017/2/15.
 */
@Spi
public interface Router {
    /**
     * invokers路由
     *
     * @param invokers 可用invokers
     * @return 路由过滤后的可用invokers
     */
    List<ReferenceInvoker> router(List<ReferenceInvoker> invokers);
}
