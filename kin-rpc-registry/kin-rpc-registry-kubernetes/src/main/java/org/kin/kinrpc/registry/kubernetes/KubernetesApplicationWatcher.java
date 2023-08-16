package org.kin.kinrpc.registry.kubernetes;

import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.client.dsl.Informable;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.ApplicationInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * @author huangjianqin
 * @date 2023/8/15
 */
public class KubernetesApplicationWatcher {
    private static final Logger log = LoggerFactory.getLogger(KubernetesApplicationWatcher.class);
    /** kubernetes registry */
    private final KubernetesRegistry registry;
    /** application instance changed 逻辑处理 */
    private final BiConsumer<String, List<ApplicationInstance>> appInstancesChangedNotifier;
    /** application name */
    private final String appName;
    /** key -> namespace, value -> 该namespace下的endpoints watcher */
    private final Map<String, KubernetesEndpointsWatcher> namespace2EndpointsWatcher = new ConcurrentHashMap<>(16);
    /** kubernetes endpoints informer, 具备缓存, 定时刷新缓存和watch */
    private final SharedIndexInformer<Endpoints> endpointsInformer;

    public KubernetesApplicationWatcher(KubernetesRegistry registry,
                                        BiConsumer<String, List<ApplicationInstance>> appInstancesChangedNotifier,
                                        String appName) {
        this.registry = registry;
        this.appInstancesChangedNotifier = appInstancesChangedNotifier;
        this.appName = appName;

        this.endpointsInformer = watchEndpoints();
        init();
    }

    /**
     * 初始化
     */
    private void init() {
        for (Endpoints endpoints : this.endpointsInformer.getStore().list()) {
            createEndpointsWatcher(endpoints);
        }

        onAppInstancesChanged();
    }

    /**
     * 创建并返回{@link KubernetesEndpointsWatcher}实例
     *
     * @param endpoints kubernetes endpoints
     */
    private void createEndpointsWatcher(Endpoints endpoints) {
        namespace2EndpointsWatcher.computeIfAbsent(endpoints.getMetadata().getNamespace(),
                k -> new KubernetesEndpointsWatcher(this, endpoints));
    }

    /**
     * 移除{@code namespace}关联的{@link KubernetesEndpointsWatcher}实例
     *
     * @param namespace kubernetes namespace
     */
    private void removeEndpointsWatcher(String namespace) {
        KubernetesEndpointsWatcher endpointsWatcher = namespace2EndpointsWatcher.remove(namespace);
        if (Objects.nonNull(endpointsWatcher)) {
            endpointsWatcher.destroy();
        }
    }

    /**
     * watch service endpoint changed
     */
    private SharedIndexInformer<Endpoints> watchEndpoints() {
        String namespace = registry.getDiscoveryNamespace();
        //服务发现是否匹配任何namespace
        boolean isAnyNamespace = StringUtils.isBlank(namespace);

        Informable<Endpoints> operation;
        if (isAnyNamespace) {
            operation = registry.getClient()
                    .endpoints()
                    .inAnyNamespace()
                    .withField("metadata.name", appName);
        } else {
            operation = registry.getClient()
                    .endpoints()
                    .inNamespace(namespace)
                    .withName(appName);
        }

        return operation
                .inform(new ResourceEventHandler<Endpoints>() {
                    @Override
                    public void onAdd(Endpoints endpoints) {
                        if (log.isDebugEnabled()) {
                            log.debug("{} received endpoint added. current pod name: {}, endpoints: {}",
                                    registry.getName(), registry.getCurPodName(), endpoints);
                        }

                        createEndpointsWatcher(endpoints);
                        onAppInstancesChanged();
                    }

                    @Override
                    public void onUpdate(Endpoints oldEndpoints, Endpoints newEndpoints) {
                        if (log.isDebugEnabled()) {
                            log.debug("{} received endpoint updated. current pod name: {}, the new endpoints: {}",
                                    registry.getName(), registry.getCurPodName(), newEndpoints);
                        }

                        String oldNamespace = oldEndpoints.getMetadata().getNamespace();
                        removeEndpointsWatcher(oldNamespace);
                        createEndpointsWatcher(newEndpoints);
                        onAppInstancesChanged();
                    }

                    @Override
                    public void onDelete(Endpoints endpoints, boolean deletedFinalStateUnknown) {
                        if (log.isDebugEnabled()) {
                            log.debug("{} received endpoint deleted. current pod name: {}, endpoints: {}",
                                    registry.getName(), registry.getCurPodName(), endpoints);
                        }

                        removeEndpointsWatcher(endpoints.getMetadata().getNamespace());
                        onAppInstancesChanged();
                    }
                });
    }

    /**
     * kubernetes application instance changed
     */
    public void onAppInstancesChanged() {
        List<ApplicationInstance> appInstances = namespace2EndpointsWatcher.values().stream()
                .map(KubernetesEndpointsWatcher::getAppInstances)
                .reduce((l, r) -> {
                    ArrayList<ApplicationInstance> merge = new ArrayList<>(l);
                    merge.addAll(r);
                    return merge;
                }).orElse(Collections.emptyList());

        appInstancesChangedNotifier.accept(appName, appInstances);
    }

    /**
     * watcher destroy and clean used resources
     */
    public void destroy() {
        if (Objects.nonNull(endpointsInformer)) {
            endpointsInformer.close();
        }
        for (KubernetesEndpointsWatcher endpointsWatcher : namespace2EndpointsWatcher.values()) {
            endpointsWatcher.destroy();
        }
    }

    //getter
    public KubernetesRegistry getRegistry() {
        return registry;
    }

    public String getAppName() {
        return appName;
    }
}
