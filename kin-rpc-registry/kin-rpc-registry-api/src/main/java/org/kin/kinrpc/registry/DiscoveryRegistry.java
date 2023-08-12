package org.kin.kinrpc.registry;

import org.jctools.maps.NonBlockingHashMap;
import org.kin.framework.cache.ReferenceCountedCache;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.ApplicationInstance;
import org.kin.kinrpc.RegistryContext;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.RegistryConfig;
import org.kin.kinrpc.config.ServerConfig;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.utils.ReferenceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author huangjianqin
 * @date 2023/7/18
 */
public abstract class DiscoveryRegistry extends AbstractRegistry {
    private static final Logger log = LoggerFactory.getLogger(DiscoveryRegistry.class);

    /** 元数据-应用协议 */
    protected static final String PROTOCOL_METADATA_KEY = "protocol";
    /** 元数据-应用revision */
    protected static final String REVISION_METADATA_KEY = "revision";

    /** 注册中心唯一name, 用于log */
    private final String name;
    /** key -> sorted provideBy, value -> {@link AppInstanceWatcher}实例 */
    private final ReferenceCountedCache<String, AppInstanceWatcher> watchers = new ReferenceCountedCache<>();
    /** key -> app name, value -> {@link AppInstanceWatcher}实例 */
    private final Map<String, Set<AppInstanceWatcher>> app2Watchers = new NonBlockingHashMap<>();
    /** key -> address, value -> app metadata */
    private final Map<String, ApplicationMetadata> address2AppMetaData = new NonBlockingHashMap<>();
    /** 是否terminated */
    private volatile boolean terminated;

    protected DiscoveryRegistry(RegistryConfig config) {
        super(config);
        this.name = RegistryManager.getAlias(config);
    }

    /**
     * 构造应用元数据map
     *
     * @param appMetadata 应用元数据
     * @return 应用元数据map
     */
    protected static Map<String, String> getMetadataMap(ApplicationMetadata appMetadata) {
        Map<String, String> metadataMap = new HashMap<>(4);
        metadataMap.put(PROTOCOL_METADATA_KEY, appMetadata.getProtocol());
        metadataMap.put(REVISION_METADATA_KEY, appMetadata.getRevision());

        return metadataMap;
    }

    /**
     * 应用实例变化
     *
     * @param appName      应用名
     * @param appInstances 应用实例列表
     */
    protected final void onAppInstancesChanged(String appName, List<ApplicationInstance> appInstances) {
        if (isTerminated()) {
            return;
        }
        Set<AppInstanceWatcher> watchers = app2Watchers.get(appName);
        for (AppInstanceWatcher watcher : watchers) {
            RegistryContext.SCHEDULER.execute(() -> watcher.onAppInstancesChanged(appName, appInstances));
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

    /**
     * 注册/更新应用实例
     */
    public final void refreshAppInstances() {
        if (isTerminated()) {
            return;
        }
        for (ApplicationMetadata appMetadata : address2AppMetaData.values()) {
            RegistryContext.SCHEDULER.execute(() -> updateIfChanged(appMetadata));
        }
    }

    /**
     * 如果有变化, 则注册/更新应用实例
     */
    private void updateIfChanged(ApplicationMetadata appMetadata) {
        String latestRevision;
        synchronized (appMetadata) {
            String revision = appMetadata.getRevision();
            latestRevision = appMetadata.calOrUpdateRevision();
            if (latestRevision.equals(revision)) {
                //revision没有变化
                return;
            }
        }

        doRegister(appMetadata);
    }

    @Override
    public final void register(ServiceConfig<?> serviceConfig) {
        checkTerminated();

        if (!serviceConfig.isRegister()) {
            return;
        }

        String appName = serviceConfig.getApp().getAppName();
        String group = config.getGroup();
        log.info("register application '{}' in group '{}' to {}", appName, group, getName());

        for (ServerConfig serverConfig : serviceConfig.getServers()) {
            String address = serverConfig.getAddress();
            ApplicationMetadata appMetadata = address2AppMetaData.computeIfAbsent(address, k -> new ApplicationMetadata(appName, serverConfig));
            appMetadata.register(serviceConfig);
        }
    }

    @Override
    public final void unregister(ServiceConfig<?> serviceConfig) {
        checkTerminated();

        if (!serviceConfig.isRegister()) {
            return;
        }

        String appName = serviceConfig.getApp().getAppName();
        String group = config.getGroup();
        log.info("unregister application '{}' in group '{}' from {}", appName, group, getName());

        String service = serviceConfig.getService();
        for (ServerConfig serverConfig : serviceConfig.getServers()) {
            String address = serverConfig.getAddress();
            ApplicationMetadata appMetadata = address2AppMetaData.get(address);
            if (Objects.isNull(appMetadata)) {
                continue;
            }
            appMetadata.unregister(service);
        }
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
        //监听变化
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

        //取消监听
        unwatch(getWatchingAppNames());
    }

    @Override
    public final void destroy() {
        if (terminated) {
            return;
        }

        terminated = true;

        for (ApplicationMetadata metadata : address2AppMetaData.values()) {
            doUnregister(metadata);
        }

        doDestroy();

        log.info("{} destroyed", getName());
    }

    /**
     * 注册服务
     *
     * @param appMetadata 应用元数据信息
     */
    protected abstract void doRegister(ApplicationMetadata appMetadata);

    /**
     * 注销服务
     *
     * @param appMetadata 应用元数据信息
     */
    protected abstract void doUnregister(ApplicationMetadata appMetadata);

    /**
     * 监听应用实例变化
     */
    protected abstract void watch(Set<String> appNames);

    /**
     * 取消监听应用实例变化
     */
    protected abstract void unwatch(Set<String> appNames);

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

    /**
     * 返回{@code revision}对应的服务元数据
     *
     * @param revision 服务元数据版本
     * @return {@link ApplicationMetadata}实例
     */
    @Nullable
    public final ApplicationMetadata getServiceMetadataMap(String revision) {
        if (StringUtils.isBlank(revision)) {
            return null;
        }

        for (ApplicationMetadata appMetadata : address2AppMetaData.values()) {
            if (revision.equals(appMetadata.getRevision())) {
                return appMetadata;
            }
        }
        return null;
    }

    //getter
    public final String getName() {
        return name;
    }

    public final boolean isTerminated() {
        return terminated;
    }
}
