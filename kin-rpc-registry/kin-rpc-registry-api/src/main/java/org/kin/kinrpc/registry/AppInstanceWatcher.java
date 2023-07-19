package org.kin.kinrpc.registry;

import org.jctools.queues.atomic.MpscUnboundedAtomicArrayQueue;
import org.kin.framework.collection.Tuple;
import org.kin.framework.utils.CollectionUtils;
import org.kin.kinrpc.*;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.constants.CommonConstants;
import org.kin.kinrpc.registry.directory.Directory;
import org.kin.kinrpc.service.MetadataService;
import org.kin.kinrpc.utils.ReferenceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author huangjianqin
 * @date 2023/7/19
 */
public class AppInstanceWatcher {
    private static final Logger log = LoggerFactory.getLogger(AppInstanceWatcher.class);

    /** app name */
    private final String appName;
    /** key -> app name, value -> application instance context list */
    private volatile Map<String, Set<AppInstanceContext>> appInstanceContexts = new ConcurrentHashMap<>();
    /** 标识是否正在处理发现的应用实例 */
    private final AtomicBoolean discovering = new AtomicBoolean(false);
    /** 待处理的应用实例列表 */
    private final Queue<List<ApplicationInstance>> discoverQueue = new MpscUnboundedAtomicArrayQueue<>(8);
    /** key -> 服务唯一标识, value -> service instance changed listener list */
    private final Map<String, Set<ServiceInstanceChangedListener>> service2Listeners = new ConcurrentHashMap<>();

    public AppInstanceWatcher(String appName) {
        this.appName = appName;
    }

    /**
     * 应用实例变更
     *
     * @param appInstances 应用实例列表
     */
    protected final void onDiscovery(List<ApplicationInstance> appInstances) {
        if (terminated) {
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
     * 处理发现的应用实例
     */
    private void doDiscover() {
        //允许直接处理的最大次数, 防止discover一直占用线程(相当于死循环), 不释放
        int maxTimes = 5;

        for (int i = 0; i < maxTimes; i++) {
            try {
                if (terminated) {
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
                    //如果有新的应用实例列表, 则更新应用元数据缓存
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

    /**
     * @param appInstances 当前所有存活的应用实例
     */
    private void doDiscover(List<ApplicationInstance> appInstances) {
        Map<String, List<ApplicationInstance>> group2ActiveAppInstances = appInstances.stream().collect(Collectors.groupingBy(ApplicationInstance::group));

        if (log.isDebugEnabled()) {
            log.debug("{} discover active application instances {}", getName(), group2ActiveAppInstances);
        }

        //deep copy
        Map<String, Set<AppInstanceContext>> group2AppInstanceContexts = new HashMap<>(this.appInstanceContexts);
        group2AppInstanceContexts.replaceAll((g, v) -> new HashSet<>(group2AppInstanceContexts.get(g)));

        if (log.isDebugEnabled()) {
            log.debug("{} old application instances {}", getName(), group2AppInstanceContexts);
        }

        //新加入的application instance
        Map<String, Set<ApplicationInstance>> group2NewAppInstances = new HashMap<>();
        Map<String, Set<AppInstanceContext>> group2RmAppInstanceContexts = new HashMap<>();
        for (Map.Entry<String, List<ApplicationInstance>> entry : group2ActiveAppInstances.entrySet()) {
            String group = entry.getKey();

            Set<ApplicationInstance> newAppInstances = new HashSet<>();
            Set<AppInstanceContext> rmAppInstanceContexts = new HashSet<>();
            //copy
            List<ApplicationInstance> activeAppInstances = new ArrayList<>(entry.getValue());

            Set<AppInstanceContext> oldAppInstanceContexts = group2AppInstanceContexts.get(group);
            if (CollectionUtils.isEmpty(oldAppInstanceContexts)) {
                newAppInstances.addAll(activeAppInstances);
            } else {
                Set<ApplicationInstance> oldAppInstances = oldAppInstanceContexts.stream()
                        .map(AppInstanceContext::getInstance)
                        .collect(Collectors.toSet());
                for (ApplicationInstance activeAppInstance : activeAppInstances) {
                    if (oldAppInstances.contains(activeAppInstance)) {
                        //仍然存活
                        oldAppInstances.remove(activeAppInstance);
                    } else {
                        //新加入
                        newAppInstances.add(activeAppInstance);
                    }
                }

                //剩余的也就是离线的
                rmAppInstanceContexts.addAll(oldAppInstanceContexts.stream()
                        .filter(oam -> oldAppInstances.contains(oam.getInstance()))
                        .collect(Collectors.toList()));
            }
            if (CollectionUtils.isNonEmpty(newAppInstances)) {
                group2NewAppInstances.put(group, newAppInstances);
            }
            if (CollectionUtils.isNonEmpty(rmAppInstanceContexts)) {
                group2RmAppInstanceContexts.put(group, rmAppInstanceContexts);
            }
        }

        //new
        boolean appInstanceChanged = false;
        if (CollectionUtils.isNonEmpty(group2NewAppInstances)) {
            fetchAppInstanceMetadata(group2AppInstanceContexts, group2NewAppInstances);
            appInstanceChanged = true;
        }

        //remove
        if (CollectionUtils.isNonEmpty(group2RmAppInstanceContexts)) {
            removeInvalidAppInstanceMetadata(group2AppInstanceContexts, group2RmAppInstanceContexts);
            appInstanceChanged = true;
        }

        if (!appInstanceChanged) {
            log.info("{} discover application instances finished, nothing changed", getName());
            return;
        }

        this.appInstanceContexts = group2AppInstanceContexts;

        log.info("{} discover application instances finished, validInstances={}", getName(), group2AppInstanceContexts);

        //notify directory
        notifyAppInstanceChanged();
    }

    /**
     * 通知所有{@link Directory}应用实例变化
     */
    protected void notifyAppInstanceChanged() {
        Map<String, List<ServiceInstance>> service2Instances = new HashMap<>();
        for (Map.Entry<String, Set<AppInstanceContext>> entry : appInstanceContexts.entrySet()) {
            for (AppInstanceContext appMetadata : entry.getValue()) {
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
            Directory directory = watchers.peek(service);
            if (Objects.isNull(directory)) {
                continue;
            }

            ReferenceContext.DISCOVERY_SCHEDULER.execute(() -> directory.discover(entry.getValue()));
        }
    }

    /**
     * 并发批量拉取应用元数据
     *
     * @param group2AppInstanceContexts 应用元数据上下文缓存
     * @param group2NewAppInstances     需要拉取的应用实例
     */
    private void fetchAppInstanceMetadata(Map<String, Set<AppInstanceContext>> group2AppInstanceContexts,
                                          Map<String, Set<ApplicationInstance>> group2NewAppInstances) {
        if (log.isDebugEnabled()) {
            log.debug("{} find new application instances {}", getName(), group2NewAppInstances);
        }

        List<Supplier<Tuple<String, AppInstanceContext>>> appInstanceContextSuppliers = new ArrayList<>(group2NewAppInstances.size() * 3);
        for (Map.Entry<String, Set<ApplicationInstance>> entry : group2NewAppInstances.entrySet()) {
            String group = entry.getKey();

            Set<AppInstanceContext> appInstanceContexts = group2AppInstanceContexts.get(group);
            if (Objects.isNull(appInstanceContexts)) {
                appInstanceContexts = new HashSet<>();
                group2AppInstanceContexts.put(group, appInstanceContexts);
            }

            for (ApplicationInstance appInstance : entry.getValue()) {
                appInstanceContextSuppliers.add(() -> {
                    ReferenceConfig<MetadataService> metadataServiceReferenceConfig = ReferenceUtils.createInternalServiceReference(appInstance,
                            MetadataService.class,
                            CommonConstants.METADATA_SERVICE_NAME);
                    MetadataService metadataService = metadataServiceReferenceConfig.refer();

                    MetadataResponse metadataResponse = metadataService.metadata();
                    Map<String, ServiceMetadata> serviceMetadataMap = metadataResponse.getServiceMetadataMap();
                    List<ServiceInstance> serviceInstances = serviceMetadataMap.entrySet()
                            .stream()
                            .map(e -> {
                                Map<String, String> iServiceMetadataMap = e.getValue().getMetadata();
                                iServiceMetadataMap.put(ServiceMetadataConstants.SCHEMA_KEY, appInstance.scheme());
                                return new DefaultServiceInstance(e.getKey(), appInstance.host(), appInstance.port(), iServiceMetadataMap);
                            })
                            .collect(Collectors.toList());
                    return new Tuple<>(group,
                            new AppInstanceContext(appInstance, serviceInstances,
                                    metadataServiceReferenceConfig, metadataService));
                });
            }
        }

        for (Tuple<String, AppInstanceContext> tuple :
                DiscoveryUtils.concurrentSupply(appInstanceContextSuppliers, "fetch application instance metadata")) {
            String group = tuple.first();
            AppInstanceContext appInstanceContext = tuple.second();

            Set<AppInstanceContext> appInstanceContexts = group2AppInstanceContexts.get(group);
            if (Objects.isNull(appInstanceContexts)) {
                appInstanceContexts = new HashSet<>();
                group2AppInstanceContexts.put(group, appInstanceContexts);
            }

            appInstanceContexts.add(appInstanceContext);
        }
    }

    /**
     * 移除无效应用元数据上下文
     *
     * @param group2AppInstanceContexts   应用元数据上下文缓存
     * @param group2RmAppInstanceContexts 需要移除的应用元数据上下文
     */
    private void removeInvalidAppInstanceMetadata(Map<String, Set<AppInstanceContext>> group2AppInstanceContexts,
                                                  Map<String, Set<AppInstanceContext>> group2RmAppInstanceContexts) {
        if (log.isDebugEnabled()) {
            log.debug("{} remove invalid application instances {}", getName(), group2RmAppInstanceContexts);
        }
        for (Map.Entry<String, Set<AppInstanceContext>> entry : group2RmAppInstanceContexts.entrySet()) {
            String group = entry.getKey();
            Set<AppInstanceContext> appInstanceContexts = entry.getValue();

            for (AppInstanceContext appInstanceContext : appInstanceContexts) {
                appInstanceContext.destroy();
            }

            //移除
            group2AppInstanceContexts.get(group).removeAll(appInstanceContexts);
        }
    }

    /**
     * 添加需要监听的{@link ServiceInstanceChangedListener}实例
     *
     * @param service  服务唯一标识
     * @param listener {@link ServiceInstanceChangedListener}实例
     */
    public void addListener(String service, ServiceInstanceChangedListener listener) {
        Set<ServiceInstanceChangedListener> listeners = service2Listeners.computeIfAbsent(service, k -> new CopyOnWriteArraySet<>(4));
        listeners.add(listener);

        // TODO: 2023/7/19 notify
    }

    /**
     * 移除正在监听的{@link ServiceInstanceChangedListener}实例
     *
     * @param service  服务唯一标识
     * @param listener {@link ServiceInstanceChangedListener}实例
     */
    public void removeListener(String service, ServiceInstanceChangedListener listener) {
        Set<ServiceInstanceChangedListener> listeners = service2Listeners.get(service);
        if (Objects.isNull(listeners)) {
            return;
        }

        listeners.remove(listener);
    }

    private void

    //getter
    public String getAppName() {
        return appName;
    }
}
