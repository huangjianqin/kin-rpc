package org.kin.kinrpc.config;

import com.google.common.collect.ImmutableSet;
import org.kin.framework.collection.CopyOnWriteMap;
import org.kin.framework.utils.CollectionUtils;
import org.kin.kinrpc.IllegalConfigException;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

/**
 * 管理应用全部配置
 *
 * @author huangjianqin
 * @date 2023/7/22
 */
public final class ApplicationConfigManager {
    /** 单例 */
    private static final ApplicationConfigManager INSTANCE = new ApplicationConfigManager();

    public static ApplicationConfigManager instance() {
        return INSTANCE;
    }

    /** 应用唯一配置类型 */
    private static final Set<Class<? extends Config>> UNIQUE_CONFIG_CLASSES;
    /** 配置类型 */
    private static final Set<Class<? extends Config>> CONFIG_CLASSES;

    static {
        ImmutableSet.Builder<Class<? extends Config>> uniqueConfigClassesBuilder = ImmutableSet.builder();
        uniqueConfigClassesBuilder.add(ApplicationConfig.class)
                .add(SslConfig.class);
        UNIQUE_CONFIG_CLASSES = uniqueConfigClassesBuilder.build();

        ImmutableSet.Builder<Class<? extends Config>> configClassesBuilder = ImmutableSet.builder();
        configClassesBuilder.add(ServiceConfig.class)
                .add(ReferenceConfig.class)
                .add(RegistryConfig.class)
                .add(ExecutorConfig.class)
                .add(ServerConfig.class)
                .add(ConsumerConfig.class)
                .add(ProviderConfig.class)
                .add(MethodConfig.class);
        CONFIG_CLASSES = configClassesBuilder.build();
    }

    /** 应用唯一配置 */
    private final Map<Class<? extends Config>, Config> uniqueConfigMap = new CopyOnWriteMap<>();
    /** key -> config class, value -> {key -> config unique id or {@link SharableConfig#getId()}, value -> config instance} */
    private final Map<Class<? extends Config>, Map<String, Config>> configMap = new CopyOnWriteMap<>();

    private ApplicationConfigManager() {
    }

    /**
     * 返回允许的配置类型
     *
     * @param config 配置实例
     * @return 配置类型
     */
    @SuppressWarnings("unchecked")
    private <C extends Config> Class<C> getConfigClass(C config) {
        Class<? extends Config> type = config.getClass();
        while (!Object.class.equals(type)) {
            if (UNIQUE_CONFIG_CLASSES.contains(type) ||
                    CONFIG_CLASSES.contains(type)) {
                return (Class<C>) type;
            }

            type = (Class<? extends Config>) type.getSuperclass();
        }

        return (Class<C>) type;
    }

    /**
     * 添加配置
     *
     * @param config 配置实例
     */
    public <C extends Config> void addConfig(C config) {
        addConfig(config, null);
    }

    /**
     * 添加配置
     *
     * @param config   配置实例
     * @param idGetter 返回配置唯一标识函数
     */
    public <C extends Config> void addConfig(C config, @Nullable Function<C, String> idGetter) {
        if (Objects.isNull(config)) {
            return;
        }

        Class<? extends Config> configClass = getConfigClass(config);
        if (isUniqueConfigClass(configClass)) {
            //应用唯一配置
            Config cached = uniqueConfigMap.get(configClass);
            if (Objects.nonNull(cached) && !config.equals(cached)) {
                throw new IllegalConfigException(String.format("%s must be application unique config", configClass.getSimpleName()));
            }

            uniqueConfigMap.put(configClass, config);
        } else {
            String id;
            if (Objects.nonNull(idGetter)) {
                id = idGetter.apply(config);
            } else {
                id = Integer.toHexString(config.hashCode());
            }

            Map<String, Config> map = configMap.computeIfAbsent(configClass, k -> new CopyOnWriteMap<>());
            map.put(id, config);
        }
    }

    /**
     * 批量添加配置
     *
     * @param cs 配置列表
     */
    public <C extends Config> void addConfigs(Collection<C> cs) {
        addConfigs(cs, null);
    }

    /**
     * 批量添加配置
     *
     * @param cs       配置列表
     * @param idGetter 返回配置唯一标识函数
     */
    public <C extends Config> void addConfigs(Collection<C> cs, @Nullable Function<C, String> idGetter) {
        for (C c : cs) {
            addConfig(c, idGetter);
        }
    }

    /**
     * 返回指定类型{@code configClass}的配置, 一般是应用唯一配置
     *
     * @param configClass 配置类型
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <C extends Config> C getConfig(Class<C> configClass) {
        if (!isUniqueConfigClass(configClass)) {
            throw new IllegalConfigException(String.format("%s is not application unique config", configClass.getSimpleName()));
        }

        return (C) uniqueConfigMap.get(configClass);
    }

    /**
     * 返回指定类型{@code configClass}且配置唯一标识为{@code id}的配置, 一般是{@link SharableConfig}配置
     *
     * @param configClass 配置类型
     * @param id          配置唯一标识
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <C extends Config> C getConfig(Class<C> configClass, String id) {
        Map<String, Config> map = configMap.get(configClass);
        if (CollectionUtils.isEmpty(map)) {
            return null;
        }

        return (C) map.get(id);
    }

    /**
     * 返回指定类型{@code configClass}的配置集合, 一般是service config or reference config
     *
     * @param configClass 配置类型
     */
    @SuppressWarnings("unchecked")
    public <C extends Config> Collection<C> getConfigs(Class<C> configClass) {
        Map<String, Config> map = configMap.get(configClass);
        if (CollectionUtils.isEmpty(map)) {
            return Collections.emptyList();
        }

        return (Collection<C>) map.values();
    }

    /**
     * 返回是否是应用唯一配置类型
     *
     * @param configClass 配置类型
     * @return true表示是应用唯一配置类型
     */
    private boolean isUniqueConfigClass(Class<? extends Config> configClass) {
        return UNIQUE_CONFIG_CLASSES.contains(configClass);
    }
}
