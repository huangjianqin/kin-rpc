package org.kin.kinrpc.cluster.invoker;

import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.ExtensionLoader;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.Invocation;
import org.kin.kinrpc.ReferenceInvoker;
import org.kin.kinrpc.RpcException;
import org.kin.kinrpc.cluster.zone.ZoneDetector;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.RegistryConfig;
import org.kin.kinrpc.constants.InvocationConstants;
import org.kin.kinrpc.registry.directory.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 根据注册中心数量决定是否启动
 * 不依赖于SPI
 * <p>
 * 特殊的{@link ClusterInvoker}不是从单一服务实例集群挑选一个服务实例并发起RPC请求, 而是从多个服务实例集群有策略地挑选一个服务实例集群,
 * 然后有策略地从该服务实例集群挑选一个服务实例并发起RPC请求
 * <p>
 * {@link #directory}实例管理的{@link ReferenceInvoker}本质上都是{@link ClusterInvoker}实例
 *
 * @author huangjianqin
 * @date 2023/7/30
 */
public class ZoneAwareClusterInvoker<T> extends ClusterInvoker<T> {
    private static final Logger log = LoggerFactory.getLogger(ZoneAwareClusterInvoker.class);

    /** zone detector */
    private final ZoneDetector zoneDetector;

    public ZoneAwareClusterInvoker(ReferenceConfig<T> referenceConfig,
                                   Directory directory) {
        super(referenceConfig, null, directory);
        List<ZoneDetector> zoneDetectors = ExtensionLoader.getExtensions(ZoneDetector.class);
        if (CollectionUtils.isNonEmpty(zoneDetectors)) {
            this.zoneDetector = zoneDetectors.get(0);
        } else {
            this.zoneDetector = null;
        }
    }

    @Override
    protected void doInvoke(Invocation invocation, CompletableFuture<Object> future) {
        //1. 优先选择
        List<ReferenceInvoker<?>> invokers = directory.list();
        for (ReferenceInvoker<?> referenceInvoker : invokers) {
            ClusterInvoker<?> clusterInvoker = (ClusterInvoker<?>) referenceInvoker;
            if (clusterInvoker.isPreferred()) {
                logInvokeCluster(invocation, clusterInvoker, "");
                clusterInvoker.invoke(invocation)
                        .onFinish(future);
                return;
            }
        }

        //2. 同区域选择
        String zone = invocation.attachment(InvocationConstants.REGISTRY_ZONE_KEY, "");
        boolean zoneForce = invocation.attachment(InvocationConstants.REGISTRY_ZONE_FORCE_KEY, false);
        if (StringUtils.isBlank(zone) && Objects.nonNull(zoneDetector)) {
            zone = zoneDetector.getZone(invocation);
            zoneForce = zoneDetector.isZoneForcing(invocation, zone);
        }

        if (StringUtils.isNotBlank(zone)) {
            List<RegistryConfig> registryConfigs = new ArrayList<>(invokers.size());
            for (ReferenceInvoker<?> referenceInvoker : invokers) {
                ClusterInvoker<?> clusterInvoker = (ClusterInvoker<?>) referenceInvoker;
                registryConfigs.add(clusterInvoker.getRegistryConfig());
                if (zone.equals(clusterInvoker.getZone())) {
                    logInvokeCluster(invocation, clusterInvoker, zone);
                    clusterInvoker.invoke(invocation)
                            .onFinish(future);
                    return;
                }
            }

            if (zoneForce) {
                future.completeExceptionally(
                        new RpcException(String.format("no registry instance in zone or no available reference in the registry, zone=%s, registries=%s, invocation=%s",
                                zone, registryConfigs, invocation)));
                return;
            }
        }

        //3. 根据权重选择
        ReferenceInvoker<?> loadBalancedInvoker = loadBalance.loadBalance(invocation, invokers);
        if (Objects.nonNull(loadBalancedInvoker)) {
            logInvokeCluster(invocation, (ClusterInvoker<?>) loadBalancedInvoker, zone);
            loadBalancedInvoker.invoke(invocation)
                    .onFinish(future);
            return;
        }

        //4. 可用选择
        if (CollectionUtils.isNonEmpty(invokers)) {
            ReferenceInvoker<?> randomInvoker = invokers.get(ThreadLocalRandom.current().nextInt(invokers.size()));
            logInvokeCluster(invocation, (ClusterInvoker<?>) randomInvoker, zone);
            randomInvoker.invoke(invocation)
                    .onFinish(future);
            return;
        }

        future.completeExceptionally(
                new RpcException(String.format("can not find any available invoker, invocation=%s", invocation)));
    }

    private void logInvokeCluster(Invocation invocation,
                                  ClusterInvoker<?> clusterInvoker,
                                  String zone) {
        RegistryConfig registryConfig = clusterInvoker.getRegistryConfig();
        log.info("zoneAware cluster select '{}' registry to send rpc call, rpcCallZone={}, registry={}, invocation={}",
                StringUtils.isNotBlank(registryConfig.getZone()) ? registryConfig.getZone() : registryConfig.getId(),
                zone, registryConfig, invocation);
    }

    @Override
    public boolean isPreferred() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getZone() {
        throw new UnsupportedOperationException();
    }

    @Override
    public RegistryConfig getRegistryConfig() {
        throw new UnsupportedOperationException();
    }
}
