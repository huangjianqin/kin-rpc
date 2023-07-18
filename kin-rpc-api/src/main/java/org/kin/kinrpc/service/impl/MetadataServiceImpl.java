package org.kin.kinrpc.service.impl;

import org.kin.framework.collection.CopyOnWriteMap;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.MetadataResponse;
import org.kin.kinrpc.ServiceMetadata;
import org.kin.kinrpc.ServiceMetadataConstants;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.service.MetadataService;

import java.util.HashMap;
import java.util.Map;

/**
 * @author huangjianqin
 * @date 2023/7/17
 */
public class MetadataServiceImpl implements MetadataService {
    /** key -> 服务唯一标识, value -> 服务元数据 */
    private final Map<String, ServiceMetadata> serviceMetadataMap = new CopyOnWriteMap<>(() -> new HashMap<>(4));

    /**
     * 注册暴露服务的元数据
     *
     * @param serviceConfig 服务配置
     */
    public synchronized void register(ServiceConfig<?> serviceConfig) {
        Map<String, String> metadata = new HashMap<>(4);
        metadata.put(ServiceMetadataConstants.SERIALIZATION_KEY, serviceConfig.getSerialization());
        metadata.put(ServiceMetadataConstants.WEIGHT_KEY, serviceConfig.getWeight().toString());
        if (StringUtils.isNotBlank(serviceConfig.getToken())) {
            metadata.put(ServiceMetadataConstants.TOKEN_KEY, serviceConfig.getToken());
        }

        serviceMetadataMap.put(serviceConfig.getService(), new ServiceMetadata(metadata));
    }

    /**
     * 注销服务的元数据
     *
     * @param service 服务唯一标识
     */
    public void unregister(String service) {
        serviceMetadataMap.remove(service);
    }

    @Override
    public MetadataResponse metadata() {
        return new MetadataResponse(serviceMetadataMap);
    }
}
