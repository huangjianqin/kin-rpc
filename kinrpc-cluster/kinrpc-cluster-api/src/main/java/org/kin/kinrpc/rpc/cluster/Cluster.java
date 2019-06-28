package org.kin.kinrpc.rpc.cluster;


import com.google.common.net.HostAndPort;
import org.kin.kinrpc.rpc.invoker.AbstractReferenceInvoker;

import java.util.Collection;

/**
 * Created by 健勤 on 2017/2/13.
 * 支持容错
 */
public interface Cluster {
    /**
     * 从注册中心中多个invoker中选择一个,包含router, loadbalance
     * @param excludes
     */
    AbstractReferenceInvoker get(Collection<HostAndPort> excludes);

    void shutdown();
}
