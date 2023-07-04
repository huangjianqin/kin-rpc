package org.kin.kinrpc.cluster.router;


import org.kin.framework.utils.SPI;
import org.kin.kinrpc.ReferenceInvoker;

import java.util.List;

/**
 * Created by 健勤 on 2017/2/15.
 */
@SPI(alias = "router")
public interface Router {
    /**
     * invokers路由规则
     *
     * @param invokers 可用invokers
     * @return 应用路由规则过滤后的可用invokers
     */
    List<ReferenceInvoker<?>> route(List<ReferenceInvoker<?>> invokers);
}
