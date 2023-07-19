package org.kin.kinrpc.registry;

import org.kin.kinrpc.ServiceInstance;

import java.util.List;

/**
 * @author huangjianqin
 * @date 2023/7/19
 */
public interface ServiceInstanceChangedListener {
    /**
     * 服务实例变化时触发
     *
     * @param serviceInstances 当前存活的服务实例
     */
    void onServiceInstanceChanged(List<ServiceInstance> serviceInstances);
}
