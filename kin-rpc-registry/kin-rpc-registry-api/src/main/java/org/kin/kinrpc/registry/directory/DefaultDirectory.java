package org.kin.kinrpc.registry.directory;

import com.google.common.base.Preconditions;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.framework.utils.ExtensionLoader;
import org.kin.kinrpc.ReferenceInvoker;
import org.kin.kinrpc.ServiceInstance;
import org.kin.kinrpc.registry.RegistryHelper;
import org.kin.kinrpc.rpc.AsyncInvoker;
import org.kin.kinrpc.rpc.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author huangjianqin
 * @date 2019/6/11
 */
public class DefaultDirectory implements Directory {
    private static final Logger log = LoggerFactory.getLogger(DefaultDirectory.class);

    /** 服务唯一标识 */
    private final String service;
    /** 订阅的invoker列表 */
    private volatile List<ReferenceInvoker<?>> invokers = Collections.emptyList();
    private volatile boolean isStopped;

    public DefaultDirectory(String service) {
        this.service = service;
    }

    /**
     * 返回已订阅且活跃的invoker列表
     */
    private List<ReferenceInvoker<?>> getActiveInvokers() {
        return invokers.stream().filter(ReferenceInvoker::isAvailable).collect(Collectors.toList());
    }

    @Override
    public List<ReferenceInvoker<?>> list() {
        if (isStopped) {
            return Collections.emptyList();
        }
        return getActiveInvokers();
    }

    @Override
    public void discover(List<ServiceInstance> serviceInstances) {
        if (isStopped) {
            return;
        }

        List<ReferenceInvoker<?>> oldInvokers = new ArrayList<>(invokers);

        List<String> oldInstanceUrls = oldInvokers.stream()
                .map(ReferenceInvoker::serviceInstance)
                .map(RegistryHelper::toSimpleUrlStr)
                .collect(Collectors.toList());
        List<String> discoverInstanceUrls = serviceInstances.stream()
                .map(RegistryHelper::toSimpleUrlStr)
                .collect(Collectors.toList());
        log.info("directory(service={}) discover start, oldInstances={}, discoverInstances={}", service, oldInstanceUrls, discoverInstanceUrls);

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

        //new instance
        for (ServiceInstance instance : serviceInstances) {
            //todo 检查protocol, serialization
            Protocol protocol = ExtensionLoader.getExtension(Protocol.class, protocolName);

            Preconditions.checkNotNull(protocol, String.format("unknown protocol: %s", protocolName));

            AsyncInvoker referenceInvoker = null;
            try {
                referenceInvoker = protocol.refer(url);
            } catch (Throwable throwable) {
                ExceptionUtils.throwExt(throwable);
            }
            validInvokers.add(referenceInvoker);
        }

        //destory invalid invokers
        for (ReferenceInvoker<?> invoker : invalidInvokers) {
            invoker.destroy();
        }

        //update cache
        this.invokers = validInvokers;

        List<String> validInstanceUrls = validInvokers.stream()
                .map(ReferenceInvoker::serviceInstance)
                .map(RegistryHelper::toSimpleUrlStr)
                .collect(Collectors.toList());

        log.info("directory(service={}) discover finish, validInstances={}", service, validInstanceUrls);
    }

    @Override
    public void destroy() {
        if (isStopped) {
            return;
        }

        isStopped = true;
        for (ReferenceInvoker<?> invoker : invokers) {
            invoker.destroy();
        }
        invokers = Collections.emptyList();
        log.info("directory destroyed");
    }

    @Override
    public String service() {
        return service;
    }
}
