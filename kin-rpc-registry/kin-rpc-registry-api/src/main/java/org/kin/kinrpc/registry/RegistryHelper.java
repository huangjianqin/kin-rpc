package org.kin.kinrpc.registry;

import org.kin.framework.cache.ReferenceCountedCache;
import org.kin.framework.utils.ExtensionLoader;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.ServiceInstance;
import org.kin.kinrpc.ServiceMetadataConstants;
import org.kin.kinrpc.common.Url;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.RegistryConfig;
import org.kin.kinrpc.config.ServerConfig;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.registry.directory.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

/**
 * Created by huangjianqin on 2019/6/18.
 */
public class RegistryHelper {
    private static final Logger log = LoggerFactory.getLogger(RegistryHelper.class);
    /** 注册中心缓存 */
    private static final ReferenceCountedCache<String, Registry> REGISTRY_CACHE = new ReferenceCountedCache<>();

    private RegistryHelper() {
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
    public static synchronized Registry getRegistry(RegistryConfig config) {
        String key = getAlias(config);
        return REGISTRY_CACHE.get(key, () -> {
            String type = config.getType();
            RegistryFactory registryFactory = ExtensionLoader.getExtension(RegistryFactory.class, type);
            if (Objects.isNull(registryFactory)) {
                throw new RegistryFactoryNotFoundException(type);
            }

            Registry registry = wrapRegistry(registryFactory.create(config), config);
            registry.init();
            return registry;
        });
    }

    /**
     * 对{@link Registry#destroy()}进行封装, 释放registry引用, 而不是直接destroy registry
     *
     * @param registry registry
     * @param config   registry config
     * @return wrapped registry instance
     */
    private static Registry wrapRegistry(Registry registry, RegistryConfig config) {
        return new Registry() {
            @Override
            public void init() {
                registry.init();
            }

            @Override
            public void register(ServiceConfig<?> serviceConfig) {
                registry.register(serviceConfig);
            }

            @Override
            public void unregister(ServiceConfig<?> serviceConfig) {
                registry.unregister(serviceConfig);
            }

            @Override
            public Directory subscribe(ReferenceConfig<?> config) {
                return registry.subscribe(config);
            }

            @Override
            public void unsubscribe(String service) {
                registry.unsubscribe(service);
            }

            @Override
            public void destroy() {
                if (REGISTRY_CACHE.release(getAlias(config))) {
                    registry.destroy();
                }
            }
        };
    }

    /**
     * 释放注册中心client引用次数
     *
     * @param config 注册中心配置
     */
    public static synchronized void releaseRegistry(RegistryConfig config) {
        REGISTRY_CACHE.release(getAlias(config));
    }

    /**
     * 返回注册中心存储的服务信息(url形式)
     *
     * @param serviceConfig 服务配置
     * @param serverConfig  server配置
     * @return 服务url
     */
    public static Url toUrl(ServiceConfig<?> serviceConfig, ServerConfig serverConfig) {
        Url url = new Url(serverConfig.getProtocol(),
                serverConfig.getHost(),
                serverConfig.getPort());
        url.putParam(ServiceMetadataConstants.APP_NAME_KEY, serviceConfig.getApp().getAppName());
        return url;
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
}
