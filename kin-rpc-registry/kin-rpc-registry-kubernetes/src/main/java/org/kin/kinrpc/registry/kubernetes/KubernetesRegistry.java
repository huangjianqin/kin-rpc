package org.kin.kinrpc.registry.kubernetes;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import org.kin.framework.utils.Extension;
import org.kin.framework.utils.IllegalFormatException;
import org.kin.framework.utils.JSON;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.ApplicationInstance;
import org.kin.kinrpc.DefaultApplicationInstance;
import org.kin.kinrpc.config.RegistryConfig;
import org.kin.kinrpc.registry.ApplicationMetadata;
import org.kin.kinrpc.registry.DiscoveryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author huangjianqin
 * @date 2023/8/13
 */
@Extension("k8s")
public class KubernetesRegistry extends DiscoveryRegistry {
    private static final Logger log = LoggerFactory.getLogger(KubernetesRegistry.class);

    /** pod metadata key */
    public final static String KUBERNETES_METADATA_KEY = "org.kinrpc/metadata";
    /** fabric8io kubernetes client */
    private final KubernetesClient client;
    /** 当前app host name */
    private final String currentHostname;
    /** user define kubernetes namespace */
    private final String namespace;
    /** key -> app name, value -> kubernetes service informer, 具备缓存, 定时刷新缓存和watch */
    private final Map<String, SharedIndexInformer<Service>> serviceInformerMap = new ConcurrentHashMap<>(16);
    /** key -> app name, value -> kubernetes pod informer, 具备缓存, 定时刷新缓存和watch */
    private final Map<String, SharedIndexInformer<Pod>> podsInformerMap = new ConcurrentHashMap<>(16);
    /** key -> app name, value -> kubernetes endpoints informer, 具备缓存, 定时刷新缓存和watch */
    private final Map<String, SharedIndexInformer<Endpoints>> endpointsInformerMap = new ConcurrentHashMap<>(16);

    public KubernetesRegistry(RegistryConfig config) {
        super(config);
        Config base = Config.autoConfigure(null);
        Config clientConfig = new ConfigBuilder(base)
                .withMasterUrl(config.attachment(KubernetesConstants.CLIENT_MASTER_URL_KEY, base.getMasterUrl()))
                .withApiVersion(config.attachment(KubernetesConstants.CLIENT_API_VERSION_KEY, base.getApiVersion()))
                .withNamespace(config.attachment(KubernetesConstants.CLIENT_NAMESPACE_KEY, base.getNamespace()))
                .withUsername(config.attachment(KubernetesConstants.CLIENT_USERNAME_KEY, base.getUsername()))
                .withPassword(config.attachment(KubernetesConstants.CLIENT_PASSWORD_KEY, base.getPassword()))

                .withOauthToken(config.attachment(KubernetesConstants.CLIENT_OAUTH_TOKEN_KEY, base.getOauthToken()))
                .withCaCertFile(config.attachment(KubernetesConstants.CLIENT_CA_CERT_FILE_KEY, base.getCaCertFile()))
                .withCaCertData(config.attachment(KubernetesConstants.CLIENT_CA_CERT_DATA_KEY, base.getCaCertData()))

                .withClientKeyFile(config.attachment(KubernetesConstants.CLIENT_KEY_FILE_KEY, base.getClientKeyFile()))
                .withClientKeyData(config.attachment(KubernetesConstants.CLIENT_KEY_DATA_KEY, base.getClientKeyData()))

                .withClientCertFile(config.attachment(KubernetesConstants.CLIENT_CERT_FILE_KEY, base.getClientCertFile()))
                .withClientCertData(config.attachment(KubernetesConstants.CLIENT_CERT_DATA_KEY, base.getClientCertData()))

                .withClientKeyAlgo(config.attachment(KubernetesConstants.CLIENT_KEY_ALGO_KEY, base.getClientKeyAlgo()))
                .withClientKeyPassphrase(config.attachment(KubernetesConstants.CLIENT_KEY_PASSPHRASE_KEY, base.getClientKeyPassphrase()))
                .withConnectionTimeout(config.intAttachment(KubernetesConstants.CLIENT_CONNECTION_TIMEOUT_KEY, base.getConnectionTimeout()))
                .withRequestTimeout(config.intAttachment(KubernetesConstants.CLIENT_REQUEST_TIMEOUT_KEY, base.getRequestTimeout()))
                .withTrustCerts(config.boolAttachment(KubernetesConstants.CLIENT_TRUST_CERTS_KEY, base.isTrustCerts()))
                .withHttpProxy(config.attachment(KubernetesConstants.CLIENT_HTTP_PROXY_KEY, base.getHttpProxy()))
                .withHttpsProxy(config.attachment(KubernetesConstants.CLIENT_HTTPS_PROXY_KEY, base.getHttpsProxy()))
                .withProxyUsername(config.attachment(KubernetesConstants.CLIENT_PROXY_USERNAME_KEY, base.getProxyUsername()))
                .withProxyPassword(config.attachment(KubernetesConstants.CLIENT_PROXY_PASSWORD_KEY, base.getProxyPassword()))
                .withNoProxy(config.attachment(KubernetesConstants.CLIENT_NO_PROXY_KEY, o -> {
                    if (o instanceof String) {
                        return o.toString().trim().split(",");
                    } else if (o.getClass().isArray() && String.class.equals(o.getClass().getComponentType())) {
                        return (String[]) o;
                    } else {
                        throw new IllegalFormatException(String.format("attachment '%s' is not a string array", KubernetesConstants.CLIENT_NO_PROXY_KEY));
                    }
                }))
                .build();

        if (StringUtils.isNotBlank(clientConfig.getNamespace())) {
            log.warn("no namespace has been detected. please specify KUBERNETES_NAMESPACE env var, or use a later kubernetes version (1.3 or later)");
        }

        this.client = new KubernetesClientBuilder()
                .withConfig(clientConfig)
                .build();

        this.currentHostname = System.getenv("HOSTNAME");
        boolean availableAccess;
        try {
            availableAccess = Objects.nonNull(client.pods().withName(currentHostname).get());
        } catch (Throwable e) {
            availableAccess = false;
        }

        if (!availableAccess) {
            log.error("unable to access api server. please check your url config. master URL: {}, hostname: {}", clientConfig.getMasterUrl(), currentHostname);
        }

        this.namespace = config.attachment(KubernetesConstants.DISCOVERY_NAMESPACE_KEY, KubernetesConstants.DEFAULT_DISCOVERY_NAMESPACE);
    }

    @Override
    public void init() {
        //do nothing
    }

    @Override
    protected void doRegister(ApplicationMetadata appMetadata) {
        DefaultApplicationInstance.Builder instance = DefaultApplicationInstance.create()
                .revision(appMetadata.getRevision())
                .scheme(appMetadata.getProtocol());

        client.pods()
                .inNamespace(namespace)
                .withName(currentHostname)
                .edit(pod -> new PodBuilder(pod)
                        .editOrNewMetadata()
                        .addToAnnotations(KUBERNETES_METADATA_KEY, JSON.write(instance.build()))
                        .endMetadata()
                        .build());
        if (log.isDebugEnabled()) {
            log.debug("{} write metadata to kubernetes pod. current pod name: {}", getName(), currentHostname);
        }
    }

    @Override
    protected void doUnregister(ApplicationMetadata appMetadata) {
        client.pods()
                .inNamespace(namespace)
                .withName(currentHostname)
                .edit(pod -> new PodBuilder(pod)
                        .editOrNewMetadata()
                        .removeFromAnnotations(KUBERNETES_METADATA_KEY)
                        .endMetadata()
                        .build());
        if (log.isDebugEnabled()) {
            log.debug("{} remove metadata from kubernetes pod. current pod name: : {}", getName(), currentHostname);
        }
    }

    @Override
    protected void watch(Set<String> appNames) {
        for (String appName : appNames) {
            // watch service endpoint changed
            endpointsInformerMap.computeIfAbsent(appName, this::watchEndpoints);

            // watch pods changed, which happens when service instance updated
            podsInformerMap.computeIfAbsent(appName, this::watchPods);

            // watch service changed, which happens when service selector updated, used to update pods watcher
            serviceInformerMap.computeIfAbsent(appName, this::watchService);
        }
    }

    /**
     * watch service endpoint changed
     *
     * @param appName 服务应用名
     */
    private SharedIndexInformer<Endpoints> watchEndpoints(String appName) {
        return client
                .endpoints()
                .inNamespace(namespace)
                .withName(appName)
                .inform(new ResourceEventHandler<Endpoints>() {
                    @Override
                    public void onAdd(Endpoints endpoints) {
                        if (log.isDebugEnabled()) {
                            log.debug("{} received endpoint added. current pod name: {}, endpoints: {}",
                                    getName(), currentHostname, endpoints);
                        }

                        onAppInstancesChanged(appName, getApplicationInstanceList(endpoints, appName));
                    }

                    @Override
                    public void onUpdate(Endpoints oldEndpoints, Endpoints newEndpoints) {
                        if (log.isDebugEnabled()) {
                            log.debug("{} received endpoint updated. current pod name: {}, the new endpoints: {}",
                                    getName(), currentHostname, newEndpoints);
                        }

                        onAppInstancesChanged(appName, getApplicationInstanceList(newEndpoints, appName));
                    }

                    @Override
                    public void onDelete(Endpoints endpoints, boolean deletedFinalStateUnknown) {
                        if (log.isDebugEnabled()) {
                            log.debug("{} received endpoint deleted. current pod name: {}, endpoints: {}",
                                    getName(), currentHostname, endpoints);
                        }

                        onAppInstancesChanged(appName, getApplicationInstanceList(endpoints, appName));
                    }
                });
    }

    /**
     * 返回service selector
     *
     * @param appName 服务应用名
     * @return service selector
     */
    private Map<String, String> getServiceSelector(String appName) {
        Service service = client.services()
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
     * 返回{@code appName}服务的所有应用实例
     *
     * @param endpoints service endpoints
     * @param appName   service application name
     * @return {@code appName}服务的所有应用实例
     */
    private List<ApplicationInstance> getApplicationInstanceList(Endpoints endpoints, String appName) {
        Map<String, String> serviceSelector = getServiceSelector(appName);
        if (serviceSelector == null) {
            return Collections.emptyList();
        }

        // key -> pod metadata name, value -> pod
        Map<String, Pod> pods = client
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
        List<ApplicationInstance> instances = new LinkedList<>();

        //遍历所有所有ApplicationInstance
        for (EndpointSubset subset : endpoints.getSubsets()) {
            for (EndpointAddress address : subset.getAddresses()) {
                Pod pod = pods.get(address.getTargetRef().getName());
                String ip = address.getIp();
                if (pod == null) {
                    log.warn("{} unable to match kubernetes endpoint address with pod. endpointAddress hostname: {}", getName(), address.getTargetRef().getName());
                    continue;
                }

                String metadata = pod.getMetadata().getAnnotations().get(KUBERNETES_METADATA_KEY);
                if (StringUtils.isNotBlank(metadata)) {
                    DefaultApplicationInstance appInstanceMetadata = JSON.read(metadata, DefaultApplicationInstance.class);
                    for (EndpointPort port : subset.getPorts()) {
                        if (StringUtils.isBlank(port.getName())) {
                            continue;
                        }

                        DefaultApplicationInstance.Builder builder = DefaultApplicationInstance.create();
                        builder.host(ip)
                                .port(port.getPort())
                                .scheme(appInstanceMetadata.scheme())
                                .revision(appInstanceMetadata.revision())
                                .metadata(appInstanceMetadata.metadata());
                        instances.add(builder.build());
                    }
                } else {
                    log.warn("{} unable to find service instance metadata in pod annotations. " +
                            "possibly cause: provider has not been initialized successfully. " +
                            "endpoint address hostname: {}", getName(), address.getTargetRef().getName());
                }
            }
        }

        return instances;
    }

    /**
     * watch pod changed
     *
     * @param appName 服务应用名
     */
    private SharedIndexInformer<Pod> watchPods(String appName) {
        Map<String, String> serviceSelector = getServiceSelector(appName);
        if (serviceSelector == null) {
            throw new IllegalStateException(String.format("can not find service selector for service '%s'", appName));
        }

        return client
                .pods()
                .inNamespace(namespace)
                .withLabels(serviceSelector)
                .inform(new ResourceEventHandler<Pod>() {
                    @Override
                    public void onAdd(Pod pod) {
                        if (log.isDebugEnabled()) {
                            log.debug("{} received pod added. current pod name: {}, pod: {}", getName(), currentHostname, pod);
                        }
                    }

                    @Override
                    public void onUpdate(Pod oldPod, Pod newPod) {
                        if (log.isDebugEnabled()) {
                            log.debug("{} received pod updated. current pod name: {}, new pod: {}", getName(), currentHostname, newPod);
                        }

                        onAppInstancesChanged(appName, getApplicationInstanceList(appName));
                    }

                    @Override
                    public void onDelete(Pod pod, boolean deletedFinalStateUnknown) {
                        if (log.isDebugEnabled()) {
                            log.debug("{} received pod deleted. current pod name: {}, pod: {}", getName(), currentHostname, pod);
                        }
                    }
                });
    }

    /**
     * 返回{@code appName}服务的所有应用实例
     *
     * @param appName service application name
     * @return {@code appName}服务的所有应用实例
     */
    private List<ApplicationInstance> getApplicationInstanceList(String appName) {
        Endpoints endpoints = null;
        SharedIndexInformer<Endpoints> endInformer = endpointsInformerMap.get(appName);
        if (endInformer != null) {
            // get endpoints directly from informer local store
            List<Endpoints> endpointsList = endInformer.getStore().list();
            if (endpointsList.size() > 0) {
                endpoints = endpointsList.get(0);
            }
        }

        if (endpoints == null) {
            //request kubernetes api service
            endpoints = client
                    .endpoints()
                    .inNamespace(namespace)
                    .withName(appName)
                    .get();
        }

        return getApplicationInstanceList(endpoints, appName);
    }

    private SharedIndexInformer<Service> watchService(String appName) {
        return client
                .services()
                .inNamespace(namespace)
                .withName(appName)
                .inform(new ResourceEventHandler<Service>() {
                            @Override
                            public void onAdd(Service service) {
                                if (log.isDebugEnabled()) {
                                    log.debug("{} received service added. current pod name: {}, service: {}", getName(), currentHostname, service);
                                }
                            }

                            @Override
                            public void onUpdate(Service oldService, Service newService) {
                                if (log.isDebugEnabled()) {
                                    log.debug("{} received service update. current pod name: {}, new service: {}", getName(), currentHostname, newService);
                                }
                                SharedIndexInformer<Pod> podInformer = podsInformerMap.remove(appName);
                                if (Objects.nonNull(podInformer)) {
                                    podInformer.close();
                                }
                                podsInformerMap.computeIfAbsent(appName, k -> watchPods(k));
                            }

                            @Override
                            public void onDelete(Service service, boolean deletedFinalStateUnknown) {
                                if (log.isDebugEnabled()) {
                                    log.debug("{} received service delete. current pod name: {}, service: {}", getName(), currentHostname, service);
                                }
                            }
                        }
                );
    }

    @Override
    protected void unwatch(Set<String> appNames) {
        for (String appName : appNames) {
            // unwatch service endpoint changed
            SharedIndexInformer<Endpoints> endpointsInformer = this.endpointsInformerMap.remove(appName);
            if (Objects.nonNull(endpointsInformer)) {
                endpointsInformer.close();
            }

            // unwatch pods changed, which happens when service instance updated
            SharedIndexInformer<Pod> podInformer = this.podsInformerMap.remove(appName);
            if (Objects.nonNull(podInformer)) {
                podInformer.close();
            }

            // unwatch service changed, which happens when service selector updated, used to update pods watcher
            SharedIndexInformer<Service> serviceInformer = this.serviceInformerMap.remove(appName);
            if (Objects.nonNull(serviceInformer)) {
                serviceInformer.close();
            }
        }
    }

    @Override
    protected void doDestroy() {
        for (SharedIndexInformer<Endpoints> informer : endpointsInformerMap.values()) {
            informer.close();
        }

        for (SharedIndexInformer<Pod> informer : podsInformerMap.values()) {
            informer.close();
        }

        for (SharedIndexInformer<Service> informer : serviceInformerMap.values()) {
            informer.close();
        }

        client.close();
    }
}
