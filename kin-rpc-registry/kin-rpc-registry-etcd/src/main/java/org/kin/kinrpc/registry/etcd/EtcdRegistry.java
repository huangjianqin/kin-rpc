package org.kin.kinrpc.registry.etcd;

import io.etcd.jetcd.*;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchResponse;
import org.kin.framework.collection.CopyOnWriteMap;
import org.kin.framework.utils.JSON;
import org.kin.framework.utils.NetUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.ApplicationInstance;
import org.kin.kinrpc.DefaultApplicationInstance;
import org.kin.kinrpc.RegistryContext;
import org.kin.kinrpc.config.RegistryConfig;
import org.kin.kinrpc.registry.ApplicationMetadata;
import org.kin.kinrpc.registry.DiscoveryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 以Etcd为注册中心, 实时监听应用实例状态变化, 并更新可用{@link org.kin.kinrpc.ReferenceInvoker}实例
 *
 * @author huangjianqin
 * @date 2023/8/12
 */
public final class EtcdRegistry extends DiscoveryRegistry {
    private static final Logger log = LoggerFactory.getLogger(EtcdRegistry.class);

    /** 分隔符 */
    private static final String SEPARATOR = ":";
    /** 默认前缀 */
    private static final String ROOT = "kinrpc" + SEPARATOR + "services";

    /** {@link #ROOT}:{@code group} */
    private static String key(String group) {
        return ROOT + SEPARATOR + group;
    }

    /** {@link #ROOT}:{@code group}:{@code appName} */
    private static String key(String group, String appName) {
        String groupPath = (StringUtils.isNotBlank(group) ? key(group) : ROOT) + SEPARATOR;
        return groupPath + appName;
    }

    /** {@link #ROOT}:{@code group}:{@code appName}:{@code address} */
    private static String key(String group, String appName, String address) {
        String groupPath = (StringUtils.isNotBlank(group) ? key(group) : ROOT) + SEPARATOR;
        return groupPath + appName + SEPARATOR + address;
    }

    /** etcd client */
    private final Client client;
    /** watcher cache */
    private final Map<String, Watch.Watcher> watcherMap = new CopyOnWriteMap<>();

    public EtcdRegistry(RegistryConfig config) {
        super(config);
        ClientBuilder builder = Client.builder();

        builder.endpoints(config.getAddressList().toArray(new String[0]));
        String user = config.attachment(EtcdConstants.USER_KEY);
        if (StringUtils.isNotBlank(user)) {
            builder.user(ByteSequence.from(user, StandardCharsets.UTF_8));
        }
        String password = config.attachment(EtcdConstants.PASSWORD_KEY);
        if (StringUtils.isNotBlank(password)) {
            builder.password(ByteSequence.from(password, StandardCharsets.UTF_8));
        }
        String authority = config.attachment(EtcdConstants.AUTHORITY_KEY);
        if (StringUtils.isNotBlank(authority)) {
            builder.authority(authority);
        }
        String namespace = config.attachment(EtcdConstants.NAMESPACE_KEY);
        if (StringUtils.isNotBlank(namespace)) {
            builder.namespace(ByteSequence.from(namespace, StandardCharsets.UTF_8));
        }
        long keepaliveTime = config.longAttachment(EtcdConstants.KEEP_ALIVE_TIME_KEY);
        if (keepaliveTime > 0) {
            builder.keepaliveTime(Duration.ofMillis(keepaliveTime));
        }
        long keepaliveTimeout = config.longAttachment(EtcdConstants.KEEP_ALIVE_TIMEOUT_KEY);
        if (keepaliveTimeout > 0) {
            builder.keepaliveTime(Duration.ofMillis(keepaliveTimeout));
        }
        boolean keepaliveWithoutCalls = config.boolAttachment(EtcdConstants.KEEP_ALIVE_WITHOUT_CALLS_KEY);
        if (keepaliveWithoutCalls) {
            builder.keepaliveWithoutCalls(true);
        }
        long connectTimeout = config.longAttachment(EtcdConstants.CONNECT_TIMEOUT_KEY);
        if (connectTimeout > 0) {
            builder.connectTimeout(Duration.ofMillis(connectTimeout));
        }
        int retryMaxAttempts = config.intAttachment(EtcdConstants.RETRY_MAX_ATTEMPTS_KEY);
        if (retryMaxAttempts > 0) {
            builder.retryMaxAttempts(retryMaxAttempts);
        }

        this.client = builder.build();
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
        String key = key(config.getGroup(), appMetadata.getAppName(), appMetadata.getAddress());
        KV kvClient = client.getKVClient();
        kvClient.put(ByteSequence.from(key, StandardCharsets.UTF_8), ByteSequence.from(JSON.writeBytes(instance.build())));
    }

    @Override
    protected void doUnregister(ApplicationMetadata appMetadata) {
        String key = key(config.getGroup(), appMetadata.getAppName(), appMetadata.getAddress());
        KV kvClient = client.getKVClient();
        kvClient.delete(ByteSequence.from(key, StandardCharsets.UTF_8));
    }

    @Override
    protected void watch(Set<String> appNames) {
        WatchOption watchOption = WatchOption.builder()
                .isPrefix(true)
                .withCreateNotify(true)
                .build();
        Watch watchClient = client.getWatchClient();
        for (String appName : appNames) {
            String key = key(config.getGroup(), appName);
            watcherMap.computeIfAbsent(key,
                    k -> watchClient.watch(ByteSequence.from(k, StandardCharsets.UTF_8), watchOption, new KvChangedListener(appName)));
        }
    }

    @Override
    protected void unwatch(Set<String> appNames) {
        Watch watchClient = client.getWatchClient();
        for (String appName : appNames) {
            String key = key(config.getGroup(), appName);
            Watch.Watcher watcher = watcherMap.remove(key);
            if (Objects.nonNull(watcher)) {
                watcher.close();
            }
        }
    }

    @Override
    protected void doDestroy() {
        for (Watch.Watcher watcher : watcherMap.values()) {
            watcher.close();
        }

        client.close();
    }

    //-----------------------------------------------------------------
    private class KvChangedListener implements Watch.Listener {
        /** watch app name */
        private final String appName;

        public KvChangedListener(String appName) {
            this.appName = appName;
        }

        @Override
        public void onNext(WatchResponse response) {
            KV kvClient = client.getKVClient();
            GetOption getOption = GetOption.builder()
                    .isPrefix(true)
                    .build();
            String key = key(config.getGroup(), appName);
            CompletableFuture<GetResponse> future = kvClient.get(ByteSequence.from(key, StandardCharsets.UTF_8), getOption);
            future.thenAcceptAsync(this::onResponse, RegistryContext.SCHEDULER);
        }

        /**
         * 处理etcd get response
         */
        private void onResponse(GetResponse response) {
            List<KeyValue> kvs = response.getKvs();
            List<ApplicationInstance> appInstances = new ArrayList<>(kvs.size());
            for (KeyValue kv : kvs) {
                String key = kv.getKey().toString(StandardCharsets.UTF_8);
                String value = kv.getValue().toString(StandardCharsets.UTF_8);
                //解析app instance address
                String[] splits = key.split(SEPARATOR);
                String address = splits[splits.length - 1];
                //组装app instance
                DefaultApplicationInstance instance = JSON.read(value, DefaultApplicationInstance.class);
                Object[] ipPortArr = NetUtils.parseIpPort(address);
                instance.setHost((String) ipPortArr[0]);
                instance.setPort((Integer) ipPortArr[1]);

                appInstances.add(instance);
            }

            onAppInstancesChanged(appName, appInstances);
        }

        @Override
        public void onError(Throwable throwable) {
            log.error("{} handle application '{}' instance changed fail", getName(), appName, throwable);
        }

        @Override
        public void onCompleted() {
            log.info("{} watch application '{}' completed", getName(), appName);
        }
    }
}
