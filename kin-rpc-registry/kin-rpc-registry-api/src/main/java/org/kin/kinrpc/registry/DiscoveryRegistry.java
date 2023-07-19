package org.kin.kinrpc.registry;

import org.kin.framework.cache.ReferenceCountedCache;
import org.kin.kinrpc.ApplicationInstance;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.RegistryConfig;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.utils.ReferenceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
    private final Map<String, Set<AppInstanceWatcher>> app2Watchers = new ConcurrentHashMap<>();
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
    private void onAppInstancesChanged(String appName, List<ApplicationInstance> appInstances) {
        AppInstanceWatcher watcher = watchers.peek(appName);
        if (Objects.isNull(watcher)) {
            return;
        }

        watcher.onDiscovery(appInstances);
    }

    @Override
    public final void register(ServiceConfig<?> serviceConfig) {
        String appName = serviceConfig.getApp().getAppName();
        String group = config.getGroup();
        log.info("register application '{}' in group '{}' to {}", appName, group, getName());

        doRegister(serviceConfig);
    }

    @Override
    public final void unregister(ServiceConfig<?> serviceConfig) {
        String appName = serviceConfig.getApp().getAppName();
        String group = config.getGroup();
        log.info("unregister application '{}' in group '{}' from {}", appName, group, getName());

        doUnregister(serviceConfig);
    }

    @Override
    public final void subscribe(ReferenceConfig<?> config, ServiceInstanceChangedListener listener) {
        String group = this.config.getGroup();
        if (log.isDebugEnabled()) {
            log.debug("reference subscribe application group '{}' on {}", group, getName());
        }

        //watch
        String provideBy = config.getProvideBy();
        Set<String> appNames = ReferenceUtils.parseProvideBy(provideBy);
        for (String appName : appNames) {
            AppInstanceWatcher watcher = watchers.get(appName, () -> new AppInstanceWatcher(appName));
            watcher.addListener(config.getService(), listener);
        }

        watch(appNames);
    }

    @Override
    public final void unsubscribe(ReferenceConfig<?> config, ServiceInstanceChangedListener listener) {
        String group = this.config.getGroup();
        if (log.isDebugEnabled()) {
            log.debug("unsubscribe application group '{}' on {}", group, getName());
        }

        //unwatch
        String provideBy = config.getProvideBy();
        Set<String> appNames = ReferenceUtils.parseProvideBy(provideBy);
        for (String appName : appNames) {
            AppInstanceWatcher watcher = watchers.peek(appName);
            watchers.release(appName);
            if (Objects.nonNull(watcher)) {
                watcher.removeListener(config.getService(), listener);
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
    protected boolean isWatching(String appName) {
        return Objects.nonNull(watchers.peek(appName));
    }

    //getter
    public String getName() {
        return name;
    }

    public boolean isTerminated() {
        return terminated;
    }
}
