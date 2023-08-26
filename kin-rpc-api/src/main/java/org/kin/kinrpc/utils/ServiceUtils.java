package org.kin.kinrpc.utils;

import com.google.common.collect.ImmutableSet;
import org.kin.framework.utils.ExtensionLoader;
import org.kin.kinrpc.ApplicationContext;
import org.kin.kinrpc.Filter;
import org.kin.kinrpc.Scope;
import org.kin.kinrpc.config.ApplicationConfig;
import org.kin.kinrpc.config.ServerConfig;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.constants.CommonConstants;
import org.kin.kinrpc.constants.Scopes;
import org.kin.kinrpc.service.MetadataService;
import org.kin.kinrpc.validation.ValidationFilter;

import java.util.*;

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

    /** 缓存spi加载的service端{@link Filter}实例 */
    private static List<Filter> serviceFilters;

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
                .app(ApplicationContext.instance().getConfig(ApplicationConfig.class))
                .server(serverConfig)
                .group(CommonConstants.INTERNAL_SERVICE_GROUP)
                .version(CommonConstants.INTERNAL_SERVICE_VERSION)
                .serviceName(CommonConstants.METADATA_SERVICE_NAME)
                //内部服务不暴露
                .register(false);
    }

    /**
     * service内部前置filter
     */
    public static List<Filter> internalPreFilters() {
        return Collections.emptyList();
    }

    /**
     * service内部后置filter
     */
    public static List<Filter> internalPostFilters() {
        return Collections.singletonList(ValidationFilter.instance());
    }

    /**
     * 返回通过spi加载的service filter
     *
     * @return service filter
     */
    public static List<Filter> getServiceFilters() {
        if (Objects.nonNull(serviceFilters)) {
            return serviceFilters;
        }

        List<Filter> filters = new ArrayList<>(4);
        for (Filter filter : ExtensionLoader.getExtensions(Filter.class)) {
            Class<? extends Filter> filterClass = filter.getClass();
            Scope scope = filterClass.getAnnotation(Scope.class);
            if (Objects.isNull(scope)) {
                continue;
            }

            if (Scopes.APPLICATION.equals(scope.value()) ||
                    Scopes.PROVIDER.equals(scope.value())) {
                filters.add(filter);
            }
        }

        serviceFilters = filters;
        return serviceFilters;
    }
}
