package org.kin.kinrpc.cluster;


import com.google.common.net.HostAndPort;
import org.kin.kinrpc.rpc.AsyncInvoker;

import java.util.Collection;

/**
 * Created by 健勤 on 2017/2/13.
 * 支持容错
 */
public interface Cluster<T> {
    /**
     * 从注册中心中多个invoker中选择一个,包含router, loadbalance
     *
     * @param excludes 需排除的invokers address
     * @return 过滤后的invoker
     */
    AsyncInvoker<T> get(Collection<HostAndPort> excludes);

    /**
     * 关闭reference
     */
    void shutdown();
}
