package org.kin.kinrpc.registry.directory;

import com.google.common.base.Preconditions;
import org.jctools.queues.atomic.MpscUnboundedAtomicArrayQueue;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.ExtensionLoader;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.ReferenceInvoker;
import org.kin.kinrpc.RegistryContext;
import org.kin.kinrpc.ServiceInstance;
import org.kin.kinrpc.ServiceMetadataConstants;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.protocol.Protocol;
import org.kin.kinrpc.protocol.Protocols;
import org.kin.kinrpc.registry.*;
import org.kin.kinrpc.transport.cmd.Serializations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 与{@link Registry}绑定的{@link Directory}实现
 *
 * @author huangjianqin
 * @date 2019/6/11
 */
public class RegistryDirectory extends AbstractDirectory implements ServiceInstanceChangedListener {
    private static final Logger log = LoggerFactory.getLogger(RegistryDirectory.class);

    /** reference config */
    private final ReferenceConfig<?> config;
    /** related registry instance */
    private final Registry registry;
    /** 订阅的invoker列表 */
    private volatile List<ReferenceInvoker<?>> invokers = Collections.emptyList();
    /** 标识是否正在处理服务发现实例 */
    private final AtomicBoolean discovering = new AtomicBoolean(false);
    /** 待处理的服务发现实例列表 */
    private final Queue<Set<ServiceInstance>> discoverQueue = new MpscUnboundedAtomicArrayQueue<>(8);
    /** 用于调用{@link #list()}阻塞等待首次服务发现完成 */
    private volatile CountDownLatch firstDiscoverWaiter = new CountDownLatch(1);
    private volatile boolean stopped;

    public RegistryDirectory(ReferenceConfig<?> config, Registry registry) {
        this.config = config;
        this.registry = registry;

        //服务订阅
        registry.subscribe(config, this);
    }

    /**
     * 返回已订阅且活跃的invoker列表
     */
    private List<ReferenceInvoker<?>> getActiveInvokers() {
        return invokers.stream()
                .filter(ReferenceInvoker::isAvailable)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReferenceInvoker<?>> list() {
        if (stopped) {
            return Collections.emptyList();
        }

        if (Objects.nonNull(firstDiscoverWaiter)) {
            try {
                firstDiscoverWaiter.await();
            } catch (InterruptedException e) {
                //ignore
            }
        }
        return getActiveInvokers();
    }

    @Override
    public void onServiceInstanceChanged(Set<ServiceInstance> serviceInstances) {
        if (stopped) {
            return;
        }

        discoverQueue.add(serviceInstances);
        if (!discovering.compareAndSet(false, true)) {
            //discovering
            return;
        }

        RegistryContext.SCHEDULER.execute(this::doDiscover);
    }

    /**
     * 处理服务发现的服务实例, 维护reference invoker缓存
     */
    private void doDiscover() {
        //允许直接处理的最大次数, 防止discover一直占用线程(相当于死循环), 不释放
        int maxTimes = 5;

        for (int i = 0; i < maxTimes; i++) {
            try {
                if (stopped) {
                    return;
                }

                //只处理最新的
                //最新的服务发现服务实例列表
                Set<ServiceInstance> lastInstances = null;
                Set<ServiceInstance> tmp;
                //遍历找到最新的服务发现服务实例列表
                while ((tmp = discoverQueue.poll()) != null) {
                    lastInstances = tmp;
                }

                if (Objects.nonNull(lastInstances)) {
                    //如果有新的服务实例, 则更新当前缓存的reference invoker
                    doDiscover(lastInstances);
                }
            } catch (Exception e) {
                log.error("directory(service={}) discover fail", service(), e);
            } finally {
                if (Objects.isNull(discoverQueue.peek())) {
                    //reset discovering flag
                    discovering.compareAndSet(true, false);
                    if (Objects.nonNull(firstDiscoverWaiter)) {
                        firstDiscoverWaiter.countDown();
                        firstDiscoverWaiter = null;
                    }
                } else {
                    //发现仍然有服务实例列表需要处理, 直接处理, 节省上下文切换
                }
            }
        }
    }

    private void doDiscover(Set<ServiceInstance> serviceInstances) {
        if (stopped) {
            return;
        }

        List<ReferenceInvoker<?>> oldInvokers = new ArrayList<>(invokers);

        List<String> oldInstanceUrls = oldInvokers.stream()
                .map(ReferenceInvoker::serviceInstance)
                .map(RegistryManager::toUrlStr)
                .collect(Collectors.toList());
        List<String> discoverInstanceUrls = serviceInstances.stream()
                .map(RegistryManager::toUrlStr)
                .collect(Collectors.toList());

        log.info("directory(service={}) discover start, oldInstances={}, discoverInstances={}", service(), oldInstanceUrls, discoverInstanceUrls);

        //过滤非法service instance, 比如不支持的序列化, 不支持的协议
        serviceInstances = serviceInstances.stream().filter(si -> {
            String serialization = si.metadata(ServiceMetadataConstants.SERIALIZATION_KEY);
            if (StringUtils.isNotBlank(serialization) &&
                    !Serializations.isSerializationExists(serialization)) {
                log.warn("directory(service={}) ignore service instance due to serialization not found, {}", service(), si);
                return false;
            }
            return true;
        }).filter(si -> {
            String schema = si.metadata(ServiceMetadataConstants.SCHEMA_KEY);
            if (StringUtils.isNotBlank(schema) &&
                    Protocols.isProtocolExists(schema)) {
                return true;
            } else {
                log.warn("directory(service={}) ignore service instance due to protocol not found, {}", service(), si);
                return false;
            }
        }).collect(Collectors.toSet());

        //遍历已创建的reference invoker, 分成3部分有效invoker, 无效invoker, 有效但未创建invoker的service instance
        List<ReferenceInvoker<?>> validInvokers = new ArrayList<>(serviceInstances.size());
        List<ReferenceInvoker<?>> invalidInvokers = new ArrayList<>(oldInvokers.size());
        if (CollectionUtils.isNonEmpty(serviceInstances)) {
            for (ReferenceInvoker<?> invoker : oldInvokers) {
                ServiceInstance instance = invoker.serviceInstance();

                if (!serviceInstances.contains(instance)) {
                    //无效invoker
                    invalidInvokers.add(invoker);
                } else {
                    //invoker仍然有效
                    validInvokers.add(invoker);
                    serviceInstances.removeIf(item -> item.equals(instance));
                }
            }
        } else {
            //如果该服务没有任何实例, 关闭所有现有的invoker
            invalidInvokers.addAll(oldInvokers);
        }

        //为未创建invoker的service instance创建invoker
        boolean serviceInstanceChanged = false;
        if (CollectionUtils.isNonEmpty(serviceInstances)) {
            validInvokers.addAll(createReferenceInvokers(serviceInstances));
            serviceInstanceChanged = true;
        }

        //async destroy invalid invokers
        if (CollectionUtils.isNonEmpty(invalidInvokers)) {
            for (ReferenceInvoker<?> invoker : invalidInvokers) {
                RegistryContext.SCHEDULER.execute(invoker::destroy);
                serviceInstanceChanged = true;
            }
        }

        if (!serviceInstanceChanged) {
            log.info("directory(service={}) discover finished, nothing changed", service());
            return;
        }

        //update cache
        this.invokers = validInvokers;

        List<ServiceInstance> finalServiceInstances = validInvokers.stream()
                .map(ReferenceInvoker::serviceInstance)
                .collect(Collectors.toList());
        List<String> finalInstanceUrls = finalServiceInstances.stream()
                .map(RegistryManager::toUrlStr)
                .collect(Collectors.toList());

        log.info("directory(service={}) discover finished, validInstances={}", service(), finalInstanceUrls);

        for (DirectoryListener listener : getListeners()) {
            try {
                listener.onDiscovery(finalServiceInstances);
            } catch (Exception e) {
                log.error("trigger {} onDiscovery error", DirectoryListener.class.getSimpleName(), e);
            }
        }
    }

    /**
     * 创建{@link ReferenceInvoker}实例
     *
     * @param serviceInstances service instance
     * @return reference invoker list
     */
    private List<ReferenceInvoker<?>> createReferenceInvokers(Set<ServiceInstance> serviceInstances) {
        if (CollectionUtils.isEmpty(serviceInstances)) {
            return Collections.emptyList();
        }

        List<Supplier<ReferenceInvoker<?>>> referenceInvokerSuppliers = new LinkedList<>();
        for (ServiceInstance instance : serviceInstances) {
            referenceInvokerSuppliers.add(() -> {
                String schema = instance.metadata(ServiceMetadataConstants.SCHEMA_KEY);
                Protocol protocol = ExtensionLoader.getExtension(Protocol.class, schema);
                Preconditions.checkNotNull(protocol, String.format("protocol not found, %s", schema));

                ReferenceInvoker<?> referenceInvoker = null;
                try {
                    referenceInvoker = protocol.refer(config, instance);
                } catch (Throwable throwable) {
                    log.error("fail to create reference invoker, instance = {}", instance);
                }
                return referenceInvoker;
            });
        }

        return DiscoveryUtils.concurrentSupply(referenceInvokerSuppliers, "create reference invoker");
    }

    @Override
    public boolean isAvailable() {
        return !stopped;
    }

    @Override
    public void destroy() {
        if (stopped) {
            return;
        }

        stopped = true;
        //取消订阅服务
        registry.unsubscribe(config, this);
        registry.destroy();

        //terminate reference invoker
        for (ReferenceInvoker<?> invoker : invokers) {
            invoker.destroy();
        }
        invokers = Collections.emptyList();

        log.info("service '{}' directory destroyed", service());
    }

    @Override
    public String service() {
        return config.getService();
    }
}
