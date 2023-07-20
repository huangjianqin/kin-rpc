package org.kin.kinrpc.registry;

import org.jctools.maps.NonBlockingHashMap;
import org.kin.framework.cache.ReferenceCountedCache;
import org.kin.framework.collection.CopyOnWriteMap;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.ApplicationInstance;
import org.kin.kinrpc.ReferenceContext;
import org.kin.kinrpc.ServiceMetadata;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.RegistryConfig;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.utils.ReferenceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author huangjianqin
 * @date 2023/7/18
 */
public abstract class DiscoveryRegistry extends AbstractRegistry {
    private static final Logger log = LoggerFactory.getLogger(DiscoveryRegistry.class);

    /** 注册中心唯一name, 用于log */
    private final String name;
    /** key -> sorted provideBy, value -> {@link AppInstanceWatcher}实例 */
    private final ReferenceCountedCache<String, AppInstanceWatcher> watchers = new ReferenceCountedCache<>();
    /** key -> app name, value -> {@link AppInstanceWatcher}实例 */
    private final Map<String, Set<AppInstanceWatcher>> app2Watchers = new NonBlockingHashMap<>();
    /** key -> 服务唯一标识, value -> 服务元数据 */
    private final Map<String, ServiceMetadata> serviceMetadataMap = new CopyOnWriteMap<>(() -> new HashMap<>(4));
    /** 是否terminated */
    private volatile boolean terminated;

    protected DiscoveryRegistry(RegistryConfig config) {
        super(config);
        this.name = RegistryHelper.getAlias(config);
    }

    /**
     * 应用实例变化
     *
     * @param appName      应用名
     * @param appInstances 应用实例列表
     */
    protected final void onAppInstancesChanged(String appName, List<ApplicationInstance> appInstances) {
        Set<AppInstanceWatcher> watchers = app2Watchers.get(appName);
        for (AppInstanceWatcher watcher : watchers) {
            ReferenceContext.DISCOVERY_SCHEDULER.execute(() -> watcher.onDiscovery(appName, appInstances));
        }
    }

    /**
     * 检查注册中心是否已经terminated
     */
    protected final void checkTerminated() {
        if (isTerminated()) {
            throw new IllegalStateException(String.format("%s has been terminated", getName()));
        }
    }

    public final void register() {

    }

    @Override
    public final void register(ServiceConfig<?> serviceConfig) {
        checkTerminated();

        String appName = serviceConfig.getApp().getAppName();
        String group = config.getGroup();
        log.info("register application '{}' in group '{}' to {}", appName, group, getName());

        doRegister(serviceConfig);
    }

    @Override
    public final void unregister(ServiceConfig<?> serviceConfig) {
        checkTerminated();

        String appName = serviceConfig.getApp().getAppName();
        String group = config.getGroup();
        log.info("unregister application '{}' in group '{}' from {}", appName, group, getName());

        doUnregister(serviceConfig);
    }

    @Override
    public final void subscribe(ReferenceConfig<?> config, ServiceInstanceChangedListener listener) {
        checkTerminated();

        String group = this.config.getGroup();
        if (log.isDebugEnabled()) {
            log.debug("reference subscribe application group '{}' on {}", group, getName());
        }

        //watch
        String provideBy = config.getProvideBy();
        Set<String> appNames = ReferenceUtils.parseProvideBy(provideBy);
        String uniqueKey = StringUtils.mkString(appNames);

        AppInstanceWatcher watcher = watchers.get(uniqueKey, () -> new AppInstanceWatcher(appNames));
        watcher.addListener(config.getService(), listener);

        Set<String> watchingAppNames = getWatchingAppNames();
        for (String appName : appNames) {
            Set<AppInstanceWatcher> watchers = app2Watchers.computeIfAbsent(appName, k -> new CopyOnWriteArraySet<>());
            watchers.add(watcher);
        }

        HashSet<String> newWatchAppNames = new HashSet<>(appNames);
        newWatchAppNames.removeIf(n -> !watchingAppNames.contains(n));
        watch(newWatchAppNames);
    }

    @Override
    public final void unsubscribe(ReferenceConfig<?> config, ServiceInstanceChangedListener listener) {
        checkTerminated();

        String group = this.config.getGroup();
        if (log.isDebugEnabled()) {
            log.debug("unsubscribe application group '{}' on {}", group, getName());
        }

        //unwatch
        String provideBy = config.getProvideBy();
        Set<String> appNames = ReferenceUtils.parseProvideBy(provideBy);
        String uniqueKey = StringUtils.mkString(appNames);

        AppInstanceWatcher watcher = watchers.peek(uniqueKey);
        if (Objects.isNull(watcher)) {
            return;
        }

        watchers.release(uniqueKey);
        watcher.removeListener(config.getService(), listener);

        for (String appName : appNames) {
            Set<AppInstanceWatcher> watchers = app2Watchers.get(appName);
            if (Objects.isNull(watchers)) {
                continue;
            }

            watchers.remove(watcher);
            if (watchers.size() < 1) {
                //没有任何watcher, 则remove
                app2Watchers.remove(appName);
            }
        }
    }

    @Override
    public final void destroy() {
        if (terminated) {
            return;
        }

        terminated = true;
        doDestroy();

        log.info("{} destroyed", getName());
    }

    /**
     * 注册服务
     *
     * @param serviceConfig 服务配置
     */
    protected abstract void doRegister(ServiceConfig<?> serviceConfig);

    /**
     * 注销服务
     *
     * @param serviceConfig 服务配置
     */
    protected abstract void doUnregister(ServiceConfig<?> serviceConfig);

    /**
     * 监听应用实例变化
     */
    protected abstract void watch(Set<String> appNames);

    /**
     * 释放注册中心占用资源
     */
    protected abstract void doDestroy();

    /**
     * 返回是否正在监听{@code appName}应用
     *
     * @return true表示正在监听{@code appName}应用
     */
    protected final boolean isWatching(String appName) {
        Set<AppInstanceWatcher> watchers = app2Watchers.get(appName);
        return Objects.nonNull(watchers) && watchers.size() > 0;
    }

    /**
     * 返回正在监听的{@code appName}应用
     *
     * @return 正在监听的{@code appName}应用集合
     */
    protected final Set<String> getWatchingAppNames() {
        return app2Watchers.keySet();
    }

    //getter
    public final String getName() {
        return name;
    }

    public final boolean isTerminated() {
        return terminated;
    }
}
