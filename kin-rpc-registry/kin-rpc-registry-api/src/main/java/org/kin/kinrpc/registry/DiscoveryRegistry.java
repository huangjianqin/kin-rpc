package org.kin.kinrpc.registry;

import org.jctools.queues.atomic.MpscUnboundedAtomicArrayQueue;
import org.kin.framework.cache.ReferenceCountedCache;
import org.kin.framework.utils.CollectionUtils;
import org.kin.kinrpc.ApplicationInstance;
import org.kin.kinrpc.ReferenceContext;
import org.kin.kinrpc.ServiceInstance;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.RegistryConfig;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.registry.directory.DefaultDirectory;
import org.kin.kinrpc.registry.directory.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * @author huangjianqin
 * @date 2023/7/18
 */
public abstract class DiscoveryRegistry extends AbstractRegistry {
    private static final Logger log = LoggerFactory.getLogger(DiscoveryRegistry.class);

    /** 注册中心唯一name, 用于log */
    private final String name;
    /**
     * {@link Directory}实例缓存
     * key -> 服务唯一标识, value -> {@link Directory}实例
     */
    private final ReferenceCountedCache<String, Directory> directoryCache = new ReferenceCountedCache<>((k, v) -> v.destroy());
    /** key -> group, value -> application metadata list */
    private volatile Map<String, Set<ApplicationMetadata>> group2AppMetadataSet = new HashMap<>();
    /** 标识是否正在处理发现的应用实例 */
    private final AtomicBoolean discovering = new AtomicBoolean(false);
    /** 待处理的应用实例列表 */
    private final Queue<List<ApplicationInstance>> discoverQueue = new MpscUnboundedAtomicArrayQueue<>(8);
    private volatile boolean stopped;

    protected DiscoveryRegistry(RegistryConfig config) {
        super(config);
        this.name = RegistryHelper.getAlias(config);
    }

    /**
     * 返回服务对应的{@link Directory}实例
     *
     * @param config reference config
     * @return {@link Directory}实例
     */
    protected final Directory getDirectory(ReferenceConfig<?> config) {
        String service = config.getService();
        return directoryCache.get(service, () -> new DefaultDirectory(config));
    }

    /**
     * 释放{@link Directory}引用计数
     *
     * @param service 服务唯一标识
     */
    protected final void freeDirectory(String service) {
        directoryCache.release(service);
    }

    /**
     * 应用实例变更
     *
     * @param appInstances 应用实例列表
     */
    protected final void onDiscovery(List<ApplicationInstance> appInstances) {
        if (stopped) {
            return;
        }

        discoverQueue.add(appInstances);
        if (!discovering.compareAndSet(false, true)) {
            //discovering
            return;
        }

        ReferenceContext.DISCOVERY_SCHEDULER.execute(this::doDiscover);
    }

    /**
     * 处理发现的应用实例, todo
     */
    private void doDiscover() {
        //允许直接处理的最大次数, 防止discover一直占用线程(相当于死循环), 不释放
        int maxTimes = 5;

        for (int i = 0; i < maxTimes; i++) {
            try {
                if (stopped) {
                    return;
                }

                //只处理最新的
                //最新发现的应用实例列表
                List<ApplicationInstance> lastAppInstances = null;
                List<ApplicationInstance> tmp;
                //遍历找到最新的应用实例列表
                while ((tmp = discoverQueue.poll()) != null) {
                    lastAppInstances = tmp;
                }

                if (Objects.nonNull(lastAppInstances)) {
                    //如果有新的应用实例列表, 则更新todo
                    doDiscover(lastAppInstances);
                }
            } catch (Exception e) {
                log.error("{} discover application fail", getName(), e);
            } finally {
                if (Objects.isNull(discoverQueue.peek())) {
                    //reset discovering flag
                    discovering.compareAndSet(true, false);

                } else {
                    //发现仍然有应用实例列表需要处理, 直接处理, 节省上下文切换
                }
            }
        }
    }

    private void doDiscover(List<ApplicationInstance> appInstances) {
        Map<String, List<ApplicationInstance>> group2ActiveAppInstances = appInstances.stream().collect(Collectors.groupingBy(ApplicationInstance::group));

        if (log.isDebugEnabled()) {
            log.debug("discover active application instances {}", group2ActiveAppInstances);
        }

        //deep copy
        Map<String, Set<ApplicationMetadata>> group2AppMetadataSet = new HashMap<>(this.group2AppMetadataSet);
        for (String group : group2AppMetadataSet.keySet()) {
            group2AppMetadataSet.put(group, new HashSet<>(group2AppMetadataSet.get(group)));
        }

        if (log.isDebugEnabled()) {
            log.debug("old application instances {}", group2AppMetadataSet);
        }

        //新加入的application instance
        Map<String, Set<ApplicationInstance>> group2NewAppInstances = new HashMap<>();
        Map<String, Set<ApplicationMetadata>> group2RmAppMetadataSet = new HashMap<>();
        for (Map.Entry<String, List<ApplicationInstance>> entry : group2ActiveAppInstances.entrySet()) {
            String group = entry.getKey();

            Set<ApplicationInstance> newAppInstances = new HashSet<>();
            Set<ApplicationMetadata> rmAppMetadataSet = new HashSet<>();
            //copy
            List<ApplicationInstance> activeAppInstances = new ArrayList<>(entry.getValue());

            Set<ApplicationMetadata> oldAppMetadataSet = group2AppMetadataSet.get(group);
            if (CollectionUtils.isEmpty(oldAppMetadataSet)) {
                newAppInstances.addAll(activeAppInstances);
            } else {
                Set<ApplicationInstance> oldAppInstances = oldAppMetadataSet.stream()
                        .map(ApplicationMetadata::getInstance)
                        .collect(Collectors.toSet());
                Iterator<ApplicationInstance> iterator = activeAppInstances.iterator();
                while (iterator.hasNext()) {
                    ApplicationInstance activeAppInstance = iterator.next();
                    if (oldAppInstances.contains(activeAppInstance)) {
                        //仍然存活
                        oldAppInstances.remove(activeAppInstance);
                    } else {
                        //新加入
                        newAppInstances.add(activeAppInstance);
                    }
                }

                //剩余的也就是离线的
                rmAppMetadataSet.addAll(oldAppMetadataSet.stream()
                        .filter(oam -> oldAppInstances.contains(oam.getInstance()))
                        .collect(Collectors.toList()));
            }

            group2NewAppInstances.put(group, newAppInstances);
            group2RmAppMetadataSet.put(group, rmAppMetadataSet);
        }

        // TODO: 2023/7/18

        //new
        if (log.isDebugEnabled()) {
            log.debug("new application instances {}", group2NewAppInstances);
        }

        //remove
        if (log.isDebugEnabled()) {
            log.debug("remove application instances {}", group2RmAppMetadataSet);
        }
        for (Map.Entry<String, Set<ApplicationMetadata>> entry : group2RmAppMetadataSet.entrySet()) {
            String group = entry.getKey();
            Set<ApplicationMetadata> appMetadataSet = entry.getValue();

            //移除
            group2AppMetadataSet.get(group).removeAll(appMetadataSet);
        }

        this.group2AppMetadataSet = group2AppMetadataSet;

        //notify directory
        Map<String, List<ServiceInstance>> service2Instances = new HashMap<>();
        for (Map.Entry<String, Set<ApplicationMetadata>> entry : group2AppMetadataSet.entrySet()) {
            for (ApplicationMetadata appMetadata : entry.getValue()) {
                for (ServiceInstance serviceInstance : appMetadata.getServiceInstances()) {
                    String service = serviceInstance.service();
                    List<ServiceInstance> serviceInstances = service2Instances.get(service);
                    if (Objects.isNull(serviceInstances)) {
                        serviceInstances = new ArrayList<>();
                        service2Instances.put(service, serviceInstances);
                    }

                    serviceInstances.add(serviceInstance);
                }
            }
        }

        for (Map.Entry<String, List<ServiceInstance>> entry : service2Instances.entrySet()) {
            String service = entry.getKey();
            Directory directory = directoryCache.peek(service);
            if (Objects.isNull(directory)) {
                continue;
            }

            ReferenceContext.DISCOVERY_SCHEDULER.execute(() -> directory.discover(entry.getValue()));
        }
    }

    @Override
    public final void register(ServiceConfig<?> serviceConfig) {
        String appName = serviceConfig.getApp().getAppName();
        String group = serviceConfig.getGroup();
        log.info("register application '{}' in group '{}' to {}", appName, group, getName());

        doRegister(serviceConfig);
    }

    @Override
    public final void unregister(ServiceConfig<?> serviceConfig) {
        String appName = serviceConfig.getApp().getAppName();
        String group = serviceConfig.getGroup();
        log.info("unregister application '{}' in group '{}' from {}", appName, group, getName());

        doUnregister(serviceConfig);
    }

    @Override
    public final Directory subscribe(ReferenceConfig<?> config) {
        String group = config.getGroup();
        if (log.isDebugEnabled()) {
            log.debug("reference subscribe application group '{}' on {}", group, getName());
        }
        return doSubscribe(config);
    }

    @Override
    public final void unsubscribe(ReferenceConfig<?> config) {
        String group = config.getGroup();
        if (log.isDebugEnabled()) {
            log.debug("unsubscribe application group '{}' on {}", group, getName());
        }

        unsubscribe(config);
    }

    @Override
    public final void destroy() {
        if (stopped) {
            return;
        }

        stopped = true;
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
     * 服务订阅
     *
     * @param config reference config
     * @return {@link Directory}实例
     */
    protected abstract Directory doSubscribe(ReferenceConfig<?> config);

    /**
     * 取消服务订阅
     *
     * @param config reference config
     */
    protected abstract void doUnsubscribe(ReferenceConfig<?> config);

    /**
     * 释放注册中心占用资源
     */
    protected abstract void doDestroy();

    //getter
    public String getName() {
        return name;
    }
}
