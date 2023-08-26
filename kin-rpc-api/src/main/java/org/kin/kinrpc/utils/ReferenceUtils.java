package org.kin.kinrpc.utils;

import org.kin.framework.utils.ExtensionLoader;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.*;
import org.kin.kinrpc.cache.CacheFilter;
import org.kin.kinrpc.common.Url;
import org.kin.kinrpc.config.ApplicationConfig;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.RegistryConfig;
import org.kin.kinrpc.config.SerializationType;
import org.kin.kinrpc.constants.CommonConstants;
import org.kin.kinrpc.constants.ReferenceConstants;
import org.kin.kinrpc.constants.Scopes;
import org.kin.kinrpc.constants.ServiceMetadataConstants;
import org.kin.kinrpc.validation.ValidationFilter;

import java.util.*;

/**
 * @author huangjianqin
 * @date 2023/7/18
 */
public final class ReferenceUtils {
    /** 缓存spi加载的reference端{@link Filter}实例 */
    private static List<Filter> referenceFilters;

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
                .app(ApplicationContext.instance().getConfig(ApplicationConfig.class));
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
     * 服务{@link Invocation}实例
     *
     * @param invocation rpc call info
     * @return {@link Invocation}新实例
     */
    public static Invocation copyInvocation(Invocation invocation) {
        RpcInvocation rpcInvocation = new RpcInvocation(invocation.serviceId(), invocation.service(),
                invocation.serviceName(), invocation.interfaceClass(),
                invocation.params(), invocation.serverAttachments(),
                invocation.methodMetadata());
        rpcInvocation.attachMany(invocation.attachments());
        return rpcInvocation;
    }

    /**
     * reference内部前置filter
     */
    public static List<Filter> internalPreFilters() {
        return Collections.emptyList();
    }

    /**
     * reference内部后置filter
     */
    public static List<Filter> internalPostFilters() {
        return Arrays.asList(ValidationFilter.instance(), CacheFilter.instance());
    }

    /**
     * 返回通过spi加载的reference filter
     *
     * @return reference filter
     */
    public static List<Filter> getReferenceFilters() {
        if (Objects.nonNull(referenceFilters)) {
            return referenceFilters;
        }

        List<Filter> filters = new ArrayList<>(4);
        for (Filter filter : ExtensionLoader.getExtensions(Filter.class)) {
            Class<? extends Filter> filterClass = filter.getClass();
            Scope scope = filterClass.getAnnotation(Scope.class);
            if (Objects.isNull(scope)) {
                continue;
            }

            if (Scopes.APPLICATION.equals(scope.value()) ||
                    Scopes.CONSUMER.equals(scope.value())) {
                filters.add(filter);
            }
        }

        referenceFilters = filters;
        return referenceFilters;
    }
}
