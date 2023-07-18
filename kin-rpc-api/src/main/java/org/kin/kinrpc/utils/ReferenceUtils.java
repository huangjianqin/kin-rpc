package org.kin.kinrpc.utils;

import org.kin.kinrpc.ApplicationInstance;
import org.kin.kinrpc.ServiceMetadataConstants;
import org.kin.kinrpc.common.Url;
import org.kin.kinrpc.config.*;
import org.kin.kinrpc.constants.CommonConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * @author huangjianqin
 * @date 2023/7/18
 */
public final class ReferenceUtils {
    private ReferenceUtils() {
    }

    /**
     * 创建内置服务reference  config
     *
     * @param appInstance    application instance
     * @param interfaceClass 服务接口
     * @return 服务reference config
     */
    public static <T> ReferenceConfig<T> createInternalServiceReference(ApplicationInstance appInstance,
                                                                        Class<T> interfaceClass) {
        return createInternalServiceReference(appInstance, interfaceClass, interfaceClass.getName());
    }

    /**
     * 创建内置服务reference  config
     *
     * @param appInstance    application instance
     * @param interfaceClass 服务接口
     * @param serviceName    服务名
     * @return 服务reference config
     */
    public static <T> ReferenceConfig<T> createInternalServiceReference(ApplicationInstance appInstance,
                                                                        Class<T> interfaceClass,
                                                                        String serviceName) {
        Map<String, String> metadata = new HashMap<>(4);
        metadata.put(ServiceMetadataConstants.SCHEMA_KEY, appInstance.scheme());
        metadata.put(ServiceMetadataConstants.SERIALIZATION_KEY, SerializationType.JSONB.getName());
        String url = new Url(appInstance.scheme(), appInstance.host(), appInstance.port(),
                GsvUtils.service(CommonConstants.INTERNAL_SERVICE_GROUP, serviceName, CommonConstants.INTERNAL_SERVICE_VERSION),
                metadata).toString();
        RegistryConfig registryConfig = RegistryConfig.direct(url);
        return ReferenceConfig.create(interfaceClass)
                .registries(registryConfig)
                .group(CommonConstants.INTERNAL_SERVICE_GROUP)
                .serviceName(serviceName)
                .version(CommonConstants.INTERNAL_SERVICE_VERSION)
                .app(ApplicationConfig.create(CommonConstants.INTERNAL_REFERENCE_APP_NAME))
                .cluster(ClusterType.FAILOVER);
    }
}
