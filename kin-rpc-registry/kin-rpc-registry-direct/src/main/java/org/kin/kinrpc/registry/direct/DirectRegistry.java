package org.kin.kinrpc.registry.direct;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.kin.kinrpc.RpcException;
import org.kin.kinrpc.ServiceInstance;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.RegistryConfig;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.registry.AbstractRegistry;
import org.kin.kinrpc.registry.RegistryHelper;
import org.kin.kinrpc.registry.directory.DefaultDirectory;
import org.kin.kinrpc.registry.directory.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * direct url, 直连
 * Created by huangjianqin on 2019/6/18.
 */
public final class DirectRegistry extends AbstractRegistry {
    private static final Logger log = LoggerFactory.getLogger(DirectRegistry.class);

    /** 配置的service instance信息 */
    private final List<ServiceInstance> serviceInstances;
    /**
     * {@link Directory}实例缓存
     * key -> 服务唯一标识, value -> {@link Directory}实例
     */
    private final Cache<String, Directory> directoryCache = CacheBuilder.newBuilder()
            .<String, Directory>removalListener(n -> n.getValue().destroy())
            .build();
    private boolean stopped;

    public DirectRegistry(RegistryConfig config) {
        super(config);

        List<String> addressList = config.getAddressList();
        List<ServiceInstance> serviceInstances = new ArrayList<>(addressList.size());
        for (String address : addressList) {
            serviceInstances.add(RegistryHelper.parseUrl(address));
        }
        this.serviceInstances = serviceInstances;
    }

    @Override
    public void init() {
        //do nothing
    }

    @Override
    public void register(ServiceConfig<?> serviceConfig) {
        //do nothing
    }

    @Override
    public void unregister(ServiceConfig<?> serviceConfig) {
        //do nothing
    }

    @Override
    public Directory subscribe(ReferenceConfig<?> config) {
        if (isStopped()) {
            throw new IllegalStateException("registry has been destroyed");
        }

        String service = config.getService();
        log.info("subscribe service '{}' ", service);
        Directory directory;
        try {
            directory = directoryCache.get(service, () -> {
                List<ServiceInstance> matchedServiceInstances = serviceInstances.stream()
                        .filter(si -> si.service().equals(service))
                        .collect(Collectors.toList());
                DefaultDirectory newDirectory = new DefaultDirectory(config);
                newDirectory.discover(matchedServiceInstances);
                return newDirectory;
            });
        } catch (ExecutionException e) {
            throw new RpcException(String.format("subscribe service '%s' fail", service), e.getCause());
        }
        return directory;
    }

    @Override
    public void unsubscribe(String service) {
        //do nothing
    }

    @Override
    public void destroy() {
        if (isStopped()) {
            return;
        }

        stopped = true;
        directoryCache.invalidateAll();
    }

    //getter
    private boolean isStopped() {
        return stopped;
    }
}
