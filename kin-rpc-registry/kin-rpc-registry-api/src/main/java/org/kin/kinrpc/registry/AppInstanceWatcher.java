package org.kin.kinrpc.registry;

import org.jctools.maps.NonBlockingHashMap;
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

    /** app name set */
    private final Set<String> appNames;
    /**
     * key -> app name, value -> application instance context list
     * value单线程更新, 多线程访问
     */
    private final Map<String, Set<AppInstanceContext>> appInstanceContexts = new NonBlockingHashMap<>();
    /** 标识是否正在处理发现的应用实例 */
    private final AtomicBoolean discovering = new AtomicBoolean(false);
    /** 待处理的应用实例列表 */
    private final Queue<Tuple<String, List<ApplicationInstance>>> discoverQueue = new MpscUnboundedAtomicArrayQueue<>(8);
    /** key -> 服务唯一标识, value -> service instance changed listener list */
    private final Map<String, Set<ServiceInstanceChangedListener>> service2Listeners = new NonBlockingHashMap<>();

    public AppInstanceWatcher(Set<String> appNames) {
        this.appNames = appNames;
    }

    /**
     * 应用实例变更触发
     *
     * @param appName      应用名
     * @param appInstances 单应用所有存活实例列表
     */
    public void onDiscovery(String appName, List<ApplicationInstance> appInstances) {
        if (CollectionUtils.isEmpty(appInstances)) {
            return;
        }

        discoverQueue.add(new Tuple<>(appName, appInstances));
        if (!discovering.compareAndSet(false, true)) {
            //discovering
            return;
        }

        doDiscover();
    }

    /**
     * 处理发现的应用实例
     */
    private void doDiscover() {
        while (true) {
            // TODO: 2023/7/20 是否可以优化执行性能, 分app fetch
            // TODO: 2023/7/20 是否可以直接缓存实例, 不用每次都遍历查询
            try {
                //只处理最新的
                //最新发现的应用实例列表
                String lastAppName;
                Tuple<String, List<ApplicationInstance>> tuple;
                //遍历找到最新的应用实例列表
                while ((tuple = discoverQueue.poll()) != null) {
                    lastAppName = tuple.first();
                    Tuple<String, List<ApplicationInstance>> headTuple = discoverQueue.peek();
                    if (Objects.isNull(headTuple)) {
                        break;
                    }

                    if (!headTuple.first().equals(lastAppName)) {
                        break;
                    }
                }

                if (Objects.nonNull(tuple)) {
                    //如果有新的应用实例列表, 则更新应用元数据缓存
                    doDiscover(tuple.first(), tuple.second());
                }
            } catch (Exception e) {
                log.error("discover application fail", e);
            }

            if (Objects.isNull(discoverQueue.peek())) {
                //reset discovering flag
                discovering.compareAndSet(true, false);
                break;
            } else {
                //发现仍然有应用实例列表需要处理, 直接处理, 节省上下文切换
            }
        }
    }

    /**
     * @param appName      应用名
     * @param appInstances 单应用所有存活实例列表
     */
    private void doDiscover(String appName, List<ApplicationInstance> appInstances) {
        //copy
        Set<AppInstanceContext> oldAppInstanceContexts = new HashSet<>(this.appInstanceContexts.computeIfAbsent(appName, k -> new HashSet<>()));
        List<ApplicationInstance> oldAppInstances = oldAppInstanceContexts.stream().map(AppInstanceContext::getInstance).collect(Collectors.toList());

        if (log.isDebugEnabled()) {
            log.debug("discover active application instances, appName={}, instances={}, oldInstances={}", appName, appInstances, oldAppInstances);
        }

        //新加入的application instance
        Set<ApplicationInstance> newAppInstances = new HashSet<>();
        Set<AppInstanceContext> invalidAppInstanceContexts = new HashSet<>();
        if (CollectionUtils.isEmpty(oldAppInstanceContexts)) {
            newAppInstances.addAll(appInstances);
        } else {
            for (ApplicationInstance appInstance : appInstances) {
                if (oldAppInstances.contains(appInstance)) {
                    //仍然存活
                    oldAppInstances.remove(appInstance);
                } else {
                    //新加入
                    newAppInstances.add(appInstance);
                }
            }
        }

        //new
        boolean appInstanceChanged = false;
        if (CollectionUtils.isNonEmpty(newAppInstances)) {
            fetchAppInstanceMetadata(appName, oldAppInstanceContexts, newAppInstances);
            appInstanceChanged = true;
        }

        //remove
        if (CollectionUtils.isNonEmpty(invalidAppInstanceContexts)) {
            removeInvalidAppInstanceMetadata(appName, oldAppInstanceContexts, invalidAppInstanceContexts);
            appInstanceChanged = true;
        }

        if (!appInstanceChanged) {
            log.info("discover application instances finished, nothing changed, appName={}", appName);
            return;
        }

        this.appInstanceContexts.put(appName, oldAppInstanceContexts);

        oldAppInstances = oldAppInstanceContexts.stream().map(AppInstanceContext::getInstance).collect(Collectors.toList());
        log.info("discover application instances finished, appName={}, instances={}", appName, oldAppInstances);

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

            Set<ServiceInstanceChangedListener> listeners = service2Listeners.get(service);
            if (CollectionUtils.isEmpty(listeners)) {
                continue;
            }

            for (ServiceInstanceChangedListener listener : listeners) {
                ReferenceContext.DISCOVERY_SCHEDULER.execute(() -> listener.onServiceInstanceChanged(entry.getValue()));
            }
        }
    }

    /**
     * 并发批量拉取应用元数据
     *
     * @param appName             应用名
     * @param appInstanceContexts 应用元数据上下文缓存
     * @param newAppInstances     需要拉取的应用实例
     */
    private void fetchAppInstanceMetadata(String appName,
                                          Set<AppInstanceContext> appInstanceContexts,
                                          Set<ApplicationInstance> newAppInstances) {
        if (log.isDebugEnabled()) {
            log.debug("find new application instances, appName={}, newInstances={}", appName, newAppInstances);
        }

        List<Supplier<AppInstanceContext>> appInstanceContextSuppliers = new ArrayList<>(newAppInstances.size());
        for (ApplicationInstance appInstance : newAppInstances) {
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
                return new AppInstanceContext(appInstance, serviceInstances,
                        metadataServiceReferenceConfig, metadataService);
            });
        }

        appInstanceContexts.addAll(
                DiscoveryUtils.concurrentSupply(appInstanceContextSuppliers, String.format("fetch application '%s' instance metadata", appName)));
    }

    /**
     * 移除无效应用元数据上下文
     *
     * @param appName                    应用名
     * @param appInstanceContexts        应用元数据上下文缓存
     * @param invalidAppInstanceContexts 无效应用元数据上下文
     */
    private void removeInvalidAppInstanceMetadata(String appName,
                                                  Set<AppInstanceContext> appInstanceContexts,
                                                  Set<AppInstanceContext> invalidAppInstanceContexts) {
        if (log.isDebugEnabled()) {
            log.debug("remove invalid application instances, appName={}, invalidInstances={}", appName, invalidAppInstanceContexts);
        }

        //destroy
        for (AppInstanceContext appInstanceContext : invalidAppInstanceContexts) {
            appInstanceContext.destroy();
        }

        //remove
        appInstanceContexts.removeAll(invalidAppInstanceContexts);
    }

    /**
     * 添加需要监听的{@link ServiceInstanceChangedListener}实例
     *
     * @param service  服务唯一标识
     * @param listener {@link ServiceInstanceChangedListener}实例
     */
    public void addListener(String service, ServiceInstanceChangedListener listener) {
        Set<ServiceInstanceChangedListener> listeners = service2Listeners.computeIfAbsent(service, k -> new CopyOnWriteArraySet<>());
        listeners.add(listener);

        notifyAppInstanceChanged();
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

    //getter
    public Set<String> getAppNames() {
        return appNames;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AppInstanceWatcher)) {
            return false;
        }
        AppInstanceWatcher that = (AppInstanceWatcher) o;
        return Objects.equals(appNames, that.appNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appNames);
    }
}
