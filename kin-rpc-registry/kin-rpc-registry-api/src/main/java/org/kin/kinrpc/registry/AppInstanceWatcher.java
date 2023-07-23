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
     * key -> app name, value -> {key -> address, value -> application instance context list}
     * value单线程更新, 多线程访问
     */
    private final Map<String, Map<String, AppInstanceContext>> appInstanceContexts = new NonBlockingHashMap<>();
    /** key -> 服务唯一标识, value -> 服务实例集合 */
    private volatile Map<String, Set<ServiceInstance>> service2Instances = new HashMap<>(8);
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
    public void onAppInstancesChanged(String appName, List<ApplicationInstance> appInstances) {
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
        Map<String, AppInstanceContext> oldAppInstanceContexts = new LinkedHashMap<>(this.appInstanceContexts.computeIfAbsent(appName, k -> new LinkedHashMap<>()));
        Set<AppInstanceContext> oldAppInstanceContextSet = new LinkedHashSet<>(oldAppInstanceContexts.values());
        Set<ApplicationInstance> oldAppInstances = oldAppInstanceContextSet
                .stream()
                .map(AppInstanceContext::getInstance)
                .collect(Collectors.toSet());

        if (log.isDebugEnabled()) {
            log.debug("discover active application instances, appName={}, instances={}, oldInstances={}", appName, appInstances, oldAppInstances);
        }

        //新加入的application instance
        Set<ApplicationInstance> newAppInstances = new HashSet<>();
        //服务元数据发生变化的application instance
        Set<AppInstanceContext> changedAppInstanceContexts = new HashSet<>();
        if (CollectionUtils.isEmpty(oldAppInstanceContexts)) {
            newAppInstances.addAll(appInstances);
        } else {
            for (ApplicationInstance appInstance : appInstances) {
                String address = appInstance.address();
                AppInstanceContext oldAppInstanceContext = oldAppInstanceContexts.get(address);
                if (Objects.nonNull(oldAppInstanceContext)) {
                    //仍然存活
                    ApplicationInstance oldAppInstance = oldAppInstanceContext.getInstance();
                    if (!appInstance.revision().equals(oldAppInstance.revision())) {
                        //服务元数据变化
                        oldAppInstanceContext.updateInstance(appInstance);
                        changedAppInstanceContexts.add(oldAppInstanceContext);
                    } else {
                        //服务元数据没有变化
                        oldAppInstanceContextSet.remove(oldAppInstanceContext);
                    }
                } else {
                    //新加入
                    newAppInstances.add(appInstance);
                }
            }
        }

        //new
        boolean appInstanceChanged = false;
        if (CollectionUtils.isNonEmpty(newAppInstances)) {
            fetchNewAppInstanceMetadata(appName, oldAppInstanceContexts, newAppInstances);
            appInstanceChanged = true;
        }

        //remove
        if (CollectionUtils.isNonEmpty(oldAppInstanceContextSet)) {
            removeInvalidAppInstanceMetadata(appName, oldAppInstanceContexts, oldAppInstanceContextSet);
            appInstanceChanged = true;
        }

        //update service metadata
        if (CollectionUtils.isNonEmpty(changedAppInstanceContexts)) {
            fetchChangedAppInstanceMetadata(appName, changedAppInstanceContexts);
            appInstanceChanged = true;
        }

        if (!appInstanceChanged) {
            log.info("discover application instances finished, nothing changed, appName={}", appName);
            return;
        }

        this.appInstanceContexts.put(appName, oldAppInstanceContexts);

        oldAppInstances = oldAppInstanceContexts.values()
                .stream()
                .map(AppInstanceContext::getInstance)
                .collect(Collectors.toSet());
        log.info("discover application instances finished, appName={}, instances={}", appName, oldAppInstances);

        //notify directory
        Map<String, Set<ServiceInstance>> oldService2Instances = this.service2Instances;
        refreshServiceInstanceCache();
        notifyIfServiceInstanceChanged(oldService2Instances, this.service2Instances);
    }

    /**
     * 并发批量拉取应用元数据
     *
     * @param appName             应用名
     * @param appInstanceContexts 应用元数据上下文缓存
     * @param newAppInstances     需要拉取的应用实例
     */
    private void fetchNewAppInstanceMetadata(String appName,
                                             Map<String, AppInstanceContext> appInstanceContexts,
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
                MetadataResponse metadataResponse = metadataService.metadata(appInstance.revision());
                List<ServiceInstance> serviceInstances = toServiceInstanceList(appInstance, metadataResponse);
                return new AppInstanceContext(appInstance, serviceInstances,
                        metadataServiceReferenceConfig, metadataService);
            });
        }

        for (AppInstanceContext appInstanceContext : DiscoveryUtils.concurrentSupply(appInstanceContextSuppliers, String.format("fetch application '%s' new instance metadata", appName))) {
            appInstanceContexts.put(appInstanceContext.address(), appInstanceContext);
        }
    }

    /**
     * 并发批量拉取应用元数据
     *
     * @param changedAppInstanceContexts 需要拉取的应用实例
     */
    private void fetchChangedAppInstanceMetadata(String appName,
                                                 Set<AppInstanceContext> changedAppInstanceContexts) {
        List<Supplier<AppInstanceContext>> appInstanceContextSuppliers = new ArrayList<>(changedAppInstanceContexts.size());
        for (AppInstanceContext appInstanceContext : changedAppInstanceContexts) {
            appInstanceContextSuppliers.add(() -> {
                ApplicationInstance appInstance = appInstanceContext.getInstance();
                MetadataResponse metadataResponse = appInstanceContext.getMetadataService()
                        .metadata(appInstanceContext.revision());
                appInstanceContext.updateServiceInstances(toServiceInstanceList(appInstance, metadataResponse));
                //return self
                return appInstanceContext;
            });
        }

        DiscoveryUtils.concurrentSupply(appInstanceContextSuppliers, String.format("fetch application '%s' changed instance metadata", appName));
    }

    /**
     * 结合{@link ApplicationInstance}应用实例和{@link MetadataResponse}服务元数据, 组装完整的服务实例信息
     *
     * @param appInstance      应用实例
     * @param metadataResponse remote返回的服务元数据
     * @return 服务实例信息列表
     */
    private List<ServiceInstance> toServiceInstanceList(ApplicationInstance appInstance, MetadataResponse metadataResponse) {
        if (Objects.isNull(metadataResponse)) {
            return Collections.emptyList();
        }
        Map<String, ServiceMetadata> serviceMetadataMap = metadataResponse.getServiceMetadataMap();
        return serviceMetadataMap.entrySet()
                .stream()
                .map(e -> {
                    Map<String, String> iServiceMetadataMap = e.getValue().getMetadata();
                    iServiceMetadataMap.put(ServiceMetadataConstants.SCHEMA_KEY, appInstance.scheme());
                    return new DefaultServiceInstance(e.getKey(), appInstance.host(), appInstance.port(), iServiceMetadataMap);
                })
                .collect(Collectors.toList());
    }

    /**
     * 移除无效应用元数据上下文
     *
     * @param appName                    应用名
     * @param appInstanceContexts        应用元数据上下文缓存
     * @param invalidAppInstanceContexts 无效应用元数据上下文
     */
    private void removeInvalidAppInstanceMetadata(String appName,
                                                  Map<String, AppInstanceContext> appInstanceContexts,
                                                  Set<AppInstanceContext> invalidAppInstanceContexts) {
        if (log.isDebugEnabled()) {
            log.debug("remove invalid application instances, appName={}, invalidInstances={}", appName, invalidAppInstanceContexts);
        }

        //destroy
        for (AppInstanceContext appInstanceContext : invalidAppInstanceContexts) {
            appInstanceContext.destroy();
        }

        //remove
        for (AppInstanceContext invalidAppInstanceContext : invalidAppInstanceContexts) {
            appInstanceContexts.remove(invalidAppInstanceContext.address());
        }
    }

    /**
     * 刷新服务实例缓存
     */
    private void refreshServiceInstanceCache() {
        Map<String, Set<ServiceInstance>> service2Instances = new HashMap<>(this.service2Instances.size() / 2 + 1);
        for (Map.Entry<String, Map<String, AppInstanceContext>> entry : appInstanceContexts.entrySet()) {
            for (AppInstanceContext appMetadataContext : entry.getValue().values()) {
                for (ServiceInstance serviceInstance : appMetadataContext.getServiceInstances()) {
                    String service = serviceInstance.service();
                    Set<ServiceInstance> serviceInstances = service2Instances.get(service);
                    if (Objects.isNull(serviceInstances)) {
                        serviceInstances = new LinkedHashSet<>();
                        service2Instances.put(service, serviceInstances);
                    }

                    serviceInstances.add(serviceInstance);
                }
            }
        }

        this.service2Instances = service2Instances;
    }

    /**
     * 如果服务实例变化, 通知所有{@link Directory}服务实例变化
     */
    private void notifyIfServiceInstanceChanged(Map<String, Set<ServiceInstance>> oldService2Instances,
                                                Map<String, Set<ServiceInstance>> service2Instances) {
        Map<String, Set<ServiceInstance>> changedServiceInstanceMap = new LinkedHashMap<>();
        for (Map.Entry<String, Set<ServiceInstance>> entry : service2Instances.entrySet()) {
            String service = entry.getKey();
            Set<ServiceInstance> serviceInstances = entry.getValue();

            boolean changed = false;
            if (oldService2Instances.containsKey(service)) {
                //检查服务实例是否一致
                Set<ServiceInstance> oldServiceInstances = oldService2Instances.get(service);
                if (oldServiceInstances.size() != serviceInstances.size()) {
                    //fast
                    changed = true;
                } else {
                    //slow, 且长度一样
                    for (ServiceInstance serviceInstance : serviceInstances) {
                        if (!oldServiceInstances.contains(serviceInstance)) {
                            changed = true;
                            break;
                        }
                    }
                }
            } else {
                //新服务
                changed = true;
            }

            if (changed) {
                changedServiceInstanceMap.put(service, serviceInstances);
            }
        }

        for (String oldService : oldService2Instances.keySet()) {
            if (service2Instances.containsKey(oldService)) {
                continue;
            }
            //老服务移除
            changedServiceInstanceMap.put(oldService, Collections.emptySet());
        }

        for (Map.Entry<String, Set<ServiceInstance>> entry : changedServiceInstanceMap.entrySet()) {
            String service = entry.getKey();

            notifyListeners(service, entry.getValue());
        }
    }

    /**
     * 通知所有{@link Directory}服务实例变化
     *
     * @param service          服务
     * @param serviceInstances 服务实例
     */
    private void notifyListeners(String service,
                                 Set<ServiceInstance> serviceInstances) {
        if (Objects.isNull(serviceInstances)) {
            return;
        }

        Set<ServiceInstanceChangedListener> listeners = service2Listeners.get(service);
        if (CollectionUtils.isEmpty(listeners)) {
            return;
        }

        for (ServiceInstanceChangedListener listener : listeners) {
            RegistryContext.SCHEDULER.execute(() -> listener.onServiceInstanceChanged(serviceInstances));
        }
    }

    /**
     * 通知所有{@link Directory}服务实例变化
     *
     * @param service 服务
     */
    private void notifyListeners(String service) {
        notifyListeners(service, this.service2Instances.get(service));
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

        notifyListeners(service);
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
