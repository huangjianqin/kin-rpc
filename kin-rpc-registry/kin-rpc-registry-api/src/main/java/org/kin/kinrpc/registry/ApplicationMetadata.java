package org.kin.kinrpc.registry;

import org.kin.kinrpc.ApplicationInstance;
import org.kin.kinrpc.ServiceInstance;

import java.util.List;
import java.util.Map;

/**
 * 应用元数据
 *
 * @author huangjianqin
 * @date 2023/7/18
 */
public class ApplicationMetadata implements ApplicationInstance {
    /** 应用实例 */
    private final ApplicationInstance instance;
    /** 该应用暴露的服务信息 */
    private final List<ServiceInstance> serviceInstances;

    public ApplicationMetadata(ApplicationInstance instance, List<ServiceInstance> serviceInstances) {
        this.instance = instance;
        this.serviceInstances = serviceInstances;
    }

    @Override
    public String group() {
        return instance.group();
    }

    @Override
    public String host() {
        return instance.host();
    }

    @Override
    public int port() {
        return instance.port();
    }

    @Override
    public Map<String, String> metadata() {
        return instance.metadata();
    }

    @Override
    public String scheme() {
        return instance.scheme();
    }

    //getter
    public ApplicationInstance getInstance() {
        return instance;
    }

    public List<ServiceInstance> getServiceInstances() {
        return serviceInstances;
    }
}
