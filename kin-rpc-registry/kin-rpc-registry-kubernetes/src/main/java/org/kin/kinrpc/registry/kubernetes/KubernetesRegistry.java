package org.kin.kinrpc.registry.kubernetes;

import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.kin.framework.utils.Extension;
import org.kin.framework.utils.IllegalFormatException;
import org.kin.framework.utils.JSON;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.DefaultApplicationInstance;
import org.kin.kinrpc.config.RegistryConfig;
import org.kin.kinrpc.registry.ApplicationMetadata;
import org.kin.kinrpc.registry.DiscoveryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
    /** 当前app host name, 即pod name */
    private final String curPodName;
    /** user define discovery kubernetes namespace */
    private final String discoveryNamespace;
    /** user define register kubernetes namespace */
    private final String registerNamespace;
    /** key -> app name, value -> app instance watcher */
    private final Map<String, KubernetesApplicationWatcher> applicationWatcherMap = new ConcurrentHashMap<>(16);

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

        this.curPodName = System.getenv("HOSTNAME");
        boolean availableAccess;
        try {
            availableAccess = Objects.nonNull(client.pods().withName(curPodName).get());
        } catch (Throwable e) {
            availableAccess = false;
        }

        if (!availableAccess) {
            log.error("unable to access api server. please check your url config. master URL: {}, hostname: {}", clientConfig.getMasterUrl(), curPodName);
        }

        this.discoveryNamespace = config.attachment(KubernetesConstants.DISCOVERY_NAMESPACE_KEY);
        this.registerNamespace = config.attachment(KubernetesConstants.REGISTER_NAMESPACE_KEY, KubernetesConstants.DEFAULT_REGISTER_NAMESPACE);
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
                .inNamespace(registerNamespace)
                .withName(curPodName)
                .edit(pod -> new PodBuilder(pod)
                        .editOrNewMetadata()
                        .addToAnnotations(KUBERNETES_METADATA_KEY, JSON.write(instance.build()))
                        .endMetadata()
                        .build());
        if (log.isDebugEnabled()) {
            log.debug("{} write metadata to kubernetes pod. current pod name: {}", getName(), curPodName);
        }
    }

    @Override
    protected void doUnregister(ApplicationMetadata appMetadata) {
        client.pods()
                .inNamespace(registerNamespace)
                .withName(curPodName)
                .edit(pod -> new PodBuilder(pod)
                        .editOrNewMetadata()
                        .removeFromAnnotations(KUBERNETES_METADATA_KEY)
                        .endMetadata()
                        .build());
        if (log.isDebugEnabled()) {
            log.debug("{} remove metadata from kubernetes pod. current pod name: : {}", getName(), curPodName);
        }
    }

    @Override
    protected void watch(Set<String> appNames) {
        for (String appName : appNames) {
            applicationWatcherMap.computeIfAbsent(appName, k ->
                    new KubernetesApplicationWatcher(
                            this, this::onAppInstancesChanged, appName));
        }
    }

    @Override
    protected void unwatch(Set<String> appNames) {
        for (String appName : appNames) {
            KubernetesApplicationWatcher appWatcher = applicationWatcherMap.get(appName);
            if (Objects.isNull(appWatcher)) {
                continue;
            }

            appWatcher.destroy();
        }
    }

    @Override
    protected void doDestroy() {
        for (KubernetesApplicationWatcher appWatcher : applicationWatcherMap.values()) {
            appWatcher.destroy();
        }

        client.close();
    }

    //getter
    public KubernetesClient getClient() {
        return client;
    }

    public String getCurPodName() {
        return curPodName;
    }

    public String getDiscoveryNamespace() {
        return discoveryNamespace;
    }

    public String getRegisterNamespace() {
        return registerNamespace;
    }
}
