package org.kin.kinrpc.cluster;


import org.kin.framework.utils.SPI;
import org.kin.kinrpc.rpc.AsyncInvoker;

import java.util.List;

/**
 * Created by 健勤 on 2017/2/15.
 */
@SPI(key = "router")
public interface Router {
    /**
     * invokers路由
     *
     * @param invokers 可用invokers
     * @return 路由过滤后的可用invokers
     */
    List<AsyncInvoker> router(List<AsyncInvoker> invokers);
}
