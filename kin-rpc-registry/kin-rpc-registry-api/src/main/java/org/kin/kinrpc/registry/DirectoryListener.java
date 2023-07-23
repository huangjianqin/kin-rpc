package org.kin.kinrpc.registry;

import org.kin.kinrpc.ServiceInstance;

import java.util.Collection;

/**
 * @author huangjianqin
 * @date 2023/7/23
 */
public interface DirectoryListener {
    /**
     * 服务发现结束时触发
     *
     * @param serviceInstances 服务示例
     */
    void onDiscovery(Collection<ServiceInstance> serviceInstances);
}
