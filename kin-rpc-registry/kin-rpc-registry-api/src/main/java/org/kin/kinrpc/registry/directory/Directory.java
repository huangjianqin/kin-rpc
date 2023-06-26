package org.kin.kinrpc.registry.directory;

import org.kin.kinrpc.ReferenceInvoker;
import org.kin.kinrpc.ServiceInstance;

import java.util.List;

/**
 * 管理订阅服务的所有invoker实例
 *
 * @author huangjianqin
 * @date 2023/6/25
 */
public interface Directory {
    /**
     * 返回当前可用invokers
     *
     * @return 可用invokers
     */
    List<ReferenceInvoker<?>> list();

    /**
     * 注册中心回调接口
     * 注册中心监听服务实例变化, 当发生变化时通知{@link Directory}
     *
     * @param serviceInstances 当前存活的服务实例
     */
    void discover(List<ServiceInstance> serviceInstances);

    /**
     * 释放reference invoker占用资源
     */
    void destroy();

    /**
     * 返回服务唯一标识
     *
     * @return 服务唯一标识
     */
    String service();
}
