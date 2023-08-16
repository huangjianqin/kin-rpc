package org.kin.kinrpc.registry.kubernetes;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import org.kin.framework.utils.JSON;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.ApplicationInstance;
import org.kin.kinrpc.DefaultApplicationInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 一kubernetes namespace下的endpoints, pods和services watcher
 *
 * @author huangjianqin
 * @date 2023/8/15
 */
public class KubernetesEndpointsWatcher {
    private static final Logger log = LoggerFactory.getLogger(KubernetesEndpointsWatcher.class);
    /** 所属application instance watcher */
    private final KubernetesApplicationWatcher appWatcher;
    /** endpoints */
    private final Endpoints endpoints;
    /** namespace */
    private final String namespace;
    /** kubernetes pod informer, 具备缓存, 定时刷新缓存和watch */
    private SharedIndexInformer<Pod> podsInformer;
    /** kubernetes service informer, 具备缓存, 定时刷新缓存和watch */
    private final SharedIndexInformer<Service> serviceInformer;
    /** endpoints下的所有application instance */
    private volatile List<ApplicationInstance> appInstances;

    public KubernetesEndpointsWatcher(KubernetesApplicationWatcher appWatcher,
                                      Endpoints endpoints) {
        this.appWatcher = appWatcher;
        this.endpoints = endpoints;
        this.namespace = endpoints.getMetadata().getNamespace();

        //watch
        podsInformer = watchPods();
        serviceInformer = watchService();

        //获取实例
        updateApplicationInstanceList(endpoints);
    }

    /**
     * 返回service selector
     *
     * @param appName 服务应用名
     * @return service selector
     */
    private Map<String, String> getServiceSelector(String appName) {
        Service service = appWatcher.getRegistry()
                .getClient()
                .services()
                .inNamespace(namespace)
                .withName(appName)
                .get();
        if (service == null) {
            return null;
        }
        return service.getSpec()
                .getSelector();
    }

    /**
     * 更新{@code appName}服务应用实例缓存
     *
     * @param endpoints service endpoints
     */
    private void updateApplicationInstanceList(Endpoints endpoints) {
        String appName = appWatcher.getAppName();
        Map<String, String> serviceSelector = getServiceSelector(appName);
        if (serviceSelector == null) {
            return;
        }

        // key -> pod metadata name, value -> pod
        KubernetesRegistry registry = appWatcher.getRegistry();
        Map<String, Pod> pods = registry
                .getClient()
                .pods()
                .inNamespace(namespace)
                .withLabels(serviceSelector)
                .list()
                .getItems()
                .stream()
                .collect(Collectors.toMap(
                        pod -> pod.getMetadata().getName(),
                        pod -> pod));

        //应用实例
        List<ApplicationInstance> appInstances = new LinkedList<>();

        //遍历所有所有ApplicationInstance
        for (EndpointSubset subset : endpoints.getSubsets()) {
            for (EndpointAddress address : subset.getAddresses()) {
                //使用EndpointAddress的ip和port作为服务的host和port, ip和port对应pod(endpoint)的真实host, 其中port本质上为service的targetPort
                Pod pod = pods.get(address.getTargetRef().getName());
                String ip = address.getIp();
                if (pod == null) {
                    log.warn("{} unable to match kubernetes endpoint address with pod. endpointAddress hostname: {}", registry.getName(), address.getTargetRef().getName());
                    continue;
                }

                String metadata = pod.getMetadata().getAnnotations().get(KubernetesRegistry.KUBERNETES_METADATA_KEY);
                if (StringUtils.isNotBlank(metadata)) {
                    DefaultApplicationInstance base = JSON.read(metadata, DefaultApplicationInstance.class);
                    for (EndpointPort port : subset.getPorts()) {
                        if (StringUtils.isBlank(port.getName())) {
                            continue;
                        }

                        DefaultApplicationInstance.Builder builder = DefaultApplicationInstance.create();
                        builder.host(ip)
                                .port(port.getPort())
                                .scheme(base.scheme())
                                .revision(base.revision())
                                .metadata(base.metadata());
                        appInstances.add(builder.build());
                    }
                } else {
                    log.warn("{} unable to find service instance metadata in pod annotations. " +
                            "possibly cause: provider has not been initialized successfully. " +
                            "endpoint address hostname: {}", registry.getName(), address.getTargetRef().getName());
                }
            }
        }

        this.appInstances = appInstances;
    }

    /**
     * watch pods changed, which happens when service instance updated
     */
    private SharedIndexInformer<Pod> watchPods() {
        String appName = appWatcher.getAppName();
        Map<String, String> serviceSelector = getServiceSelector(appName);
        if (serviceSelector == null) {
            throw new IllegalStateException(String.format("can not find service selector for service '%s'", appName));
        }

        KubernetesRegistry registry = appWatcher.getRegistry();
        return registry
                .getClient()
                .pods()
                .inNamespace(namespace)
                .withLabels(serviceSelector)
                .inform(new ResourceEventHandler<Pod>() {
                    @Override
                    public void onAdd(Pod pod) {
                        if (log.isDebugEnabled()) {
                            log.debug("{} received pod added. current pod name: {}, pod: {}", registry.getName(), registry.getCurPodName(), pod);
                        }
                    }

                    @Override
                    public void onUpdate(Pod oldPod, Pod newPod) {
                        if (log.isDebugEnabled()) {
                            log.debug("{} received pod updated. current pod name: {}, new pod: {}", registry.getName(), registry.getCurPodName(), newPod);
                        }

                        updateApplicationInstanceList(endpoints);
                        appWatcher.onAppInstancesChanged();
                    }

                    @Override
                    public void onDelete(Pod pod, boolean deletedFinalStateUnknown) {
                        if (log.isDebugEnabled()) {
                            log.debug("{} received pod deleted. current pod name: {}, pod: {}", registry.getName(), registry.getCurPodName(), pod);
                        }
                    }
                });
    }

    /**
     * watch service changed, which happens when service selector updated, used to update pods watcher
     */
    private SharedIndexInformer<Service> watchService() {
        String appName = appWatcher.getAppName();
        KubernetesRegistry registry = appWatcher.getRegistry();
        return registry
                .getClient()
                .services()
                .inNamespace(namespace)
                .withName(appName)
                .inform(new ResourceEventHandler<Service>() {
                            @Override
                            public void onAdd(Service service) {
                                if (log.isDebugEnabled()) {
                                    log.debug("{} received service added. current pod name: {}, service: {}", registry.getName(), registry.getCurPodName(), service);
                                }
                            }

                            @Override
                            public void onUpdate(Service oldService, Service newService) {
                                if (log.isDebugEnabled()) {
                                    log.debug("{} received service update. current pod name: {}, new service: {}", registry.getName(), registry.getCurPodName(), newService);
                                }
                                podsInformer.close();
                                podsInformer = watchPods();
                            }

                            @Override
                            public void onDelete(Service service, boolean deletedFinalStateUnknown) {
                                if (log.isDebugEnabled()) {
                                    log.debug("{} received service delete. current pod name: {}, service: {}", registry.getName(), registry.getCurPodName(), service);
                                }
                            }
                        }
                );
    }

    /**
     * watcher destroy and clean used resources
     */
    public void destroy() {
        // unwatch pods changed, which happens when service instance updated
        podsInformer.close();

        // unwatch service changed, which happens when service selector updated, used to update pods watcher
        serviceInformer.close();
    }

    //getter
    public Endpoints getEndpoints() {
        return endpoints;
    }

    public List<ApplicationInstance> getAppInstances() {
        return Objects.nonNull(appInstances) ? appInstances : Collections.emptyList();
    }
}
