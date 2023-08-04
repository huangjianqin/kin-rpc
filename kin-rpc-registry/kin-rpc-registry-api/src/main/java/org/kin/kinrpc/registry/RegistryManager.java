package org.kin.kinrpc.registry;

import org.kin.framework.cache.ReferenceCountedCache;
import org.kin.framework.utils.ExtensionLoader;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.DefaultServiceInstance;
import org.kin.kinrpc.ServiceInstance;
import org.kin.kinrpc.ServiceMetadataConstants;
import org.kin.kinrpc.common.Url;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.RegistryConfig;
import org.kin.kinrpc.config.ServerConfig;
import org.kin.kinrpc.config.ServiceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by huangjianqin on 2019/6/18.
 */
public class RegistryManager {
    private static final Logger log = LoggerFactory.getLogger(RegistryManager.class);
    /** 注册中心缓存 */
    private static final ReferenceCountedCache<String, RegistryEntry> REGISTRY_CACHE = new ReferenceCountedCache<>();

    private RegistryManager() {
    }

    /**
     * 获取注册中心缓存key
     *
     * @param config 注册中心配置
     * @return 注册中心缓存key
     */
    public static String getAlias(RegistryConfig config) {
        String name = config.getName();
        if (StringUtils.isNotBlank(name)) {
            return name;
        } else {
            return config.getType() + "(" + config.getAddress() + ")";
        }
    }

    /**
     * 返回注册中心, 如果缓存中已存在注册中心client, 则复用, 否则创建一个新的
     *
     * @param config 注册中心配置
     * @return {@link Registry}实例
     */
    public static synchronized Registry createRegistryIfAbsent(RegistryConfig config) {
        String alias = getAlias(config);
        return REGISTRY_CACHE.get(alias, () -> {
            String type = config.getType();
            RegistryFactory registryFactory = ExtensionLoader.getExtension(RegistryFactory.class, type);
            if (Objects.isNull(registryFactory)) {
                throw new RegistryFactoryNotFoundException(type);
            }

            RegistryEntry registryEntry = wrapRegistry(alias, registryFactory.create(config));
            registryEntry.init();
            return registryEntry;
        });
    }

    /**
     * 返回注册中心, 不增加引用计数
     *
     * @param config 注册中心配置
     * @return {@link Registry}实例
     */
    @Nullable
    public static Registry getRegistry(RegistryConfig config) {
        return REGISTRY_CACHE.peek(getAlias(config));
    }

    /**
     * 对{@link Registry#destroy()}进行封装, 释放registry引用, 而不是直接destroy registry
     *
     * @param alias    registry name
     * @param registry registry
     * @return wrapped registry instance
     */
    private static RegistryEntry wrapRegistry(String alias, Registry registry) {
        return new RegistryEntry(alias, registry);
    }

    /**
     * 返回所有已注册的{@link DiscoveryRegistry}实例
     *
     * @return 所有已注册的{@link DiscoveryRegistry}实例
     */
    public static List<DiscoveryRegistry> getDiscoveryRegistries() {
        return REGISTRY_CACHE.values()
                .stream()
                .map(RegistryEntry::getProxy)
                .filter(r -> r instanceof DiscoveryRegistry)
                .map(r -> ((DiscoveryRegistry) r))
                .collect(Collectors.toList());
    }

    /**
     * 返回注册中心存储的服务信息(url形式)
     *
     * @param serviceConfig 服务配置
     * @param serverConfig  server配置
     * @return 服务url
     */
    public static Url toUrl(ServiceConfig<?> serviceConfig, ServerConfig serverConfig) {
        return new Url(serverConfig.getProtocol(),
                serverConfig.getHost(),
                serverConfig.getPort());
    }

    /**
     * 返回服务实例url, 包含元数据
     *
     * @param instance 服务实例
     * @return 服务url string
     */
    public static String toUrlStr(ServiceInstance instance) {
        return new Url(instance.scheme(), instance.host(), instance.port(), instance.service(), instance.metadata()).toString();
    }

    /**
     * 解析url, 并封装成{@link ServiceInstance}实例
     *
     * @param urlStr 注册中心url
     * @return {@link ServiceInstance}实例
     */
    public static ServiceInstance parseUrl(String urlStr) {
        Url url = Url.of(urlStr);
        Map<String, String> params = url.getParams();
        params.put(ServiceMetadataConstants.SCHEMA_KEY, url.getProtocol());
        return new DefaultServiceInstance(url.getPath(), url.getHost(), url.getPort(), params);
    }

    //----------------------------------------------------------------------------------------------------------------------------------------------------
    private static class RegistryEntry implements Registry {
        private final String alias;
        private final Registry proxy;

        public RegistryEntry(String alias, Registry proxy) {
            this.alias = alias;
            this.proxy = proxy;
        }

        @Override
        public void init() {
            proxy.init();
        }

        @Override
        public void register(ServiceConfig<?> serviceConfig) {
            proxy.register(serviceConfig);
        }

        @Override
        public void unregister(ServiceConfig<?> serviceConfig) {
            proxy.unregister(serviceConfig);
        }

        @Override
        public void subscribe(ReferenceConfig<?> config, ServiceInstanceChangedListener listener) {
            proxy.subscribe(config, listener);
        }

        @Override
        public void unsubscribe(ReferenceConfig<?> config, ServiceInstanceChangedListener listener) {
            proxy.unsubscribe(config, listener);
        }

        @Override
        public void destroy() {
            if (REGISTRY_CACHE.release(alias)) {
                proxy.destroy();
            }
        }

        //getter
        public String getAlias() {
            return alias;
        }

        public Registry getProxy() {
            return proxy;
        }

        @Override
        public String toString() {
            return proxy.toString();
        }
    }
}
