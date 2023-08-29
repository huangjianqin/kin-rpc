package org.kin.kinrpc.registry.nacos;

import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.NetUtils;
import org.kin.kinrpc.ApplicationInstance;
import org.kin.kinrpc.DefaultApplicationInstance;
import org.kin.kinrpc.RegistryContext;
import org.kin.kinrpc.config.RegistryConfig;
import org.kin.kinrpc.registry.ApplicationMetadata;
import org.kin.kinrpc.registry.DiscoveryRegistry;
import org.kin.kinrpc.registry.RegistryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Supplier;

/**
 * 以nacos为注册中心, 实时监听应用实例状态变化, 并更新可用{@link org.kin.kinrpc.ReferenceInvoker}实例
 *
 * @author huangjianqin
 * @date 2023/8/10
 */
public final class NacosRegistry extends DiscoveryRegistry {
    private static final Logger log = LoggerFactory.getLogger(NacosRegistry.class);

    /** nacos cluster name */
    private final String clusterName;
    /** nacos服务实例是否持久化 */
    private final boolean ephemeral;
    /** nacos naming service */
    private final NamingService namingService;

    public NacosRegistry(RegistryConfig config) {
        super(config);
        clusterName = config.attachment(PropertyKeyConst.CLUSTER_NAME, NacosConstants.DEFAULT_CLUSTER_NAME);
        ephemeral = config.boolAttachment(NacosConstants.EPHEMERAL, true);
        //nacos配置, 即PropertyKeyConst.XXX
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, config.getAddress());
        properties.putAll(config.attachments());
        //set default cluster name if user not set
        properties.put(PropertyKeyConst.CLUSTER_NAME, clusterName);
        try {
            namingService = NamingFactory.createNamingService(properties);
        } catch (NacosException e) {
            throw new RegistryException(e);
        }
    }

    @Override
    public void init() {
        //do nothing
    }

    /**
     * 返回nacos service instance
     *
     * @param appMetadata kinrpc application  metadata
     * @return nacos service instance
     */
    private Instance genInstance(ApplicationMetadata appMetadata) {
        String address = appMetadata.getAddress();
        Object[] ipPort = NetUtils.parseIpPort(address);
        String protocol = appMetadata.getProtocol();
        String revision = appMetadata.getRevision();

        Instance instance = new Instance();
        instance.setIp(((String) ipPort[0]));
        instance.setPort(((Integer) ipPort[1]));
        //单机模式不支持服务实例持久化, 需要集群模式才支持, 也就是开启raft才支持服务实例持久化
        instance.setEphemeral(ephemeral);
        instance.setWeight(config.getWeight());
        //元数据
        instance.setMetadata(getMetadataMap(appMetadata));
        //cluster
        instance.setClusterName(clusterName);

        return instance;
    }

    @Override
    protected void doRegister(ApplicationMetadata appMetadata) {
        String appName = appMetadata.getAppName();
        Instance instance = genInstance(appMetadata);
        try {
            namingService.registerInstance(appName, config.getGroup(), instance);
        } catch (NacosException e) {
            throw new RegistryException(String.format("%s register service instance fail, instance=%s", getName(), instance), e);
        }
    }

    @Override
    protected void doUnregister(ApplicationMetadata appMetadata) {
        String appName = appMetadata.getAppName();
        Instance instance = genInstance(appMetadata);
        try {
            namingService.deregisterInstance(appName, config.getGroup(), instance);
        } catch (NacosException e) {
            throw new RegistryException(String.format("%s deregister service instance fail, instance=%s", getName(), instance), e);
        }
    }

    @Override
    protected void watch(Set<String> appNames) {
        //watch
        for (String appName : appNames) {
            try {
                namingService.subscribe(appName, config.getGroup(), Collections.singletonList(clusterName), event -> {
                    if (event instanceof NamingEvent) {
                        NamingEvent namingEvent = (NamingEvent) event;
                        onInstanceChanged(namingEvent.getServiceName(), namingEvent.getInstances());
                    }
                });
            } catch (NacosException e) {
                throw new RegistryException(String.format("%s subscribe fail, appName=%s", getName(), appName), e);
            }
        }

        //异步拉取service instance
        List<Supplier<List<Instance>>> suppliers = new ArrayList<>(appNames.size());
        for (String appName : appNames) {
            RegistryContext.SCHEDULER.execute(() -> {
                List<Instance> instances;
                try {
                    instances = namingService.selectInstances(appName, config.getGroup(), Collections.singletonList(clusterName), true, true);
                } catch (NacosException e) {
                    throw new RegistryException(String.format("%s selectInstances fail, appName=%s", getName(), appName), e);
                }

                if (CollectionUtils.isEmpty(instances)) {
                    return;
                }

                onInstanceChanged(appName, instances);
            });
        }
    }

    @Override
    protected void unwatch(Set<String> appNames) {
        EventListener unsubscribeListener = e -> {
        };
        for (String appName : appNames) {
            try {
                namingService.unsubscribe(appName, config.getGroup(), Collections.singletonList(clusterName), unsubscribeListener);
            } catch (NacosException e) {
                throw new RegistryException(String.format("%s unsubscribe fail, appName=%s", getName(), appName), e);
            }
        }
    }

    /**
     * nacos service instance changed
     *
     * @param instances nacos service instance
     */
    private void onInstanceChanged(String appName, List<Instance> instances) {
        List<ApplicationInstance> appInstances = new ArrayList<>(instances.size());
        for (Instance instance : instances) {
            Map<String, String> metadata = instance.getMetadata();
            DefaultApplicationInstance appInstance = DefaultApplicationInstance.create()
                    .host(instance.getIp())
                    .port(instance.getPort())
                    .revision(metadata.get(REVISION_METADATA_KEY))
                    .scheme(metadata.get(PROTOCOL_METADATA_KEY))
                    .build();
            appInstances.add(appInstance);
        }
        onAppInstancesChanged(appName, appInstances);
    }

    @Override
    protected void doDestroy() {
        try {
            namingService.shutDown();
        } catch (NacosException e) {
            throw new RegistryException(String.format("%s shutdown fail", getName()), e);
        }
    }
}
