package org.kin.kinrpc.utils;

import com.google.common.collect.ImmutableSet;
import org.kin.kinrpc.config.ApplicationConfig;
import org.kin.kinrpc.config.ApplicationConfigManager;
import org.kin.kinrpc.config.ServerConfig;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.constants.CommonConstants;
import org.kin.kinrpc.service.MetadataService;

import java.util.Set;

/**
 * @author huangjianqin
 * @date 2023/7/21
 */
public final class ServiceUtils {
    /** 内置服务接口 */
    private static final Set<Class<?>> INTERNAL_SERVICE_INTERFACES;

    static {
        ImmutableSet.Builder<Class<?>> internalServiceInterfacebuilder = ImmutableSet.builder();
        internalServiceInterfacebuilder.add(MetadataService.class);
        INTERNAL_SERVICE_INTERFACES = internalServiceInterfacebuilder.build();
    }

    private ServiceUtils() {
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

    public static <T> ServiceConfig<T> createInternalService(Class<T> type, ServerConfig serverConfig, T instance) {
        return (ServiceConfig<T>) ServiceConfig.create(type, instance)
                .app(ApplicationConfigManager.instance().getConfig(ApplicationConfig.class))
                .server(serverConfig)
                .group(CommonConstants.INTERNAL_SERVICE_GROUP)
                .version(CommonConstants.INTERNAL_SERVICE_VERSION)
                .serviceName(CommonConstants.METADATA_SERVICE_NAME)
                //内部服务不暴露
                .register(false);
    }

}
