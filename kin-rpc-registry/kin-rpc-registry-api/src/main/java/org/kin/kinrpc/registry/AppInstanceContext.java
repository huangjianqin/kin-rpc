package org.kin.kinrpc.registry;

import org.kin.kinrpc.ApplicationInstance;
import org.kin.kinrpc.ServiceInstance;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.service.MetadataService;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 应用元数据
 *
 * @author huangjianqin
 * @date 2023/7/18
 */
public class AppInstanceContext implements ApplicationInstance {
    /** 应用实例 */
    private ApplicationInstance instance;
    /** 该应用暴露的服务信息 */
    private List<ServiceInstance> serviceInstances;
    /** {@link MetadataService} reference config */
    private final ReferenceConfig<MetadataService> metadataServiceReferenceConfig;
    /** {@link MetadataService} reference */
    private final MetadataService metadataService;

    public AppInstanceContext(ApplicationInstance instance,
                              List<ServiceInstance> serviceInstances,
                              ReferenceConfig<MetadataService> metadataServiceReferenceConfig,
                              MetadataService metadataService) {
        this.instance = instance;
        this.serviceInstances = serviceInstances;
        this.metadataServiceReferenceConfig = metadataServiceReferenceConfig;
        this.metadataService = metadataService;
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

    @Override
    public String revision() {
        return instance.revision();
    }

    /**
     * 更新应用实例
     *
     * @param instance 最新应用实例
     */
    public void updateInstance(ApplicationInstance instance) {
        this.instance = instance;
    }

    /**
     * 更新服务元数据
     *
     * @param serviceInstances 服务元数据
     */
    public void updateServiceInstances(List<ServiceInstance> serviceInstances) {
        this.serviceInstances = serviceInstances;
    }

    /**
     * 释放资源
     */
    public void destroy() {
        metadataServiceReferenceConfig.unRefer();
    }

    //getter
    public ApplicationInstance getInstance() {
        return instance;
    }

    public List<ServiceInstance> getServiceInstances() {
        return serviceInstances;
    }

    public ReferenceConfig<MetadataService> getMetadataServiceReferenceConfig() {
        return metadataServiceReferenceConfig;
    }

    public MetadataService getMetadataService() {
        return metadataService;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AppInstanceContext)) return false;
        AppInstanceContext that = (AppInstanceContext) o;
        return Objects.equals(instance, that.instance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instance);
    }
}
