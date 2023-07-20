package org.kin.kinrpc.utils;

import com.google.common.collect.ImmutableSet;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.ApplicationInstance;
import org.kin.kinrpc.ServiceMetadataConstants;
import org.kin.kinrpc.common.Url;
import org.kin.kinrpc.config.*;
import org.kin.kinrpc.constants.CommonConstants;
import org.kin.kinrpc.constants.ReferenceConstants;
import org.kin.kinrpc.service.MetadataService;

import java.util.*;

/**
 * @author huangjianqin
 * @date 2023/7/18
 */
public final class ReferenceUtils {
    /** 内置服务接口 */
    private static final Set<Class<?>> INTERNAL_SERVICE_INTERFACES;

    static {
        ImmutableSet.Builder<Class<?>> internalServiceInterfacebuilder = ImmutableSet.builder();
        internalServiceInterfacebuilder.add(MetadataService.class);
        INTERNAL_SERVICE_INTERFACES = internalServiceInterfacebuilder.build();
    }

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
     * todo 如何带上ssl
     * todo app ssl 全jvm唯一
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

    /**
     * 解析{@link ReferenceConfig#getProvideBy()}字段
     *
     * @param provideBy app names collection
     * @return sorted app name set
     */
    public static Set<String> parseProvideBy(String provideBy) {
        if (StringUtils.isBlank(provideBy)) {
            return Collections.emptySet();
        }

        return new TreeSet<>(Arrays.asList(provideBy.split(ReferenceConstants.PROVIDE_BY_SEPARATOR)));
    }

    /**
     * 返回{@code type}是否是内部服务接口
     *
     * @param type 服务接口
     * @return true表示{@code type}是内部服务接口
     */
    public static boolean isInternalService(Class<?> type) {
        return INTERNAL_SERVICE_INTERFACES.contains(type);
    }
}
