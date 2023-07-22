package org.kin.kinrpc.registry.direct;

import org.kin.kinrpc.ServiceInstance;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.RegistryConfig;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.registry.AbstractRegistry;
import org.kin.kinrpc.registry.RegistryHelper;
import org.kin.kinrpc.registry.ServiceInstanceChangedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * direct url, 直连
 * Created by huangjianqin on 2019/6/18.
 */
public final class DirectRegistry extends AbstractRegistry {
    private static final Logger log = LoggerFactory.getLogger(DirectRegistry.class);

    /** 配置的service instance信息 */
    private final List<ServiceInstance> serviceInstances;
    private boolean terminated;

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
    public void subscribe(ReferenceConfig<?> config, ServiceInstanceChangedListener listener) {
        if (isTerminated()) {
            throw new IllegalStateException("DirectRegistry has been terminated");
        }

        String service = config.getService();
        Set<ServiceInstance> matchedServiceInstances = serviceInstances.stream()
                .filter(si -> si.service().equals(service))
                .collect(Collectors.toSet());

        if (log.isDebugEnabled()) {
            log.debug("subscribe service '{}' and find instances, {}", service, matchedServiceInstances);
        }

        listener.onServiceInstanceChanged(matchedServiceInstances);
    }

    @Override
    public void unsubscribe(ReferenceConfig<?> config, ServiceInstanceChangedListener listener) {
        //do nothing
    }

    @Override
    public void destroy() {
        if (isTerminated()) {
            return;
        }

        terminated = true;
    }

    //getter
    public boolean isTerminated() {
        return terminated;
    }

    @Override
    public String toString() {
        return "DirectRegistry{" +
                "serviceInstances=" + serviceInstances +
                ", terminated=" + terminated +
                "}";
    }
}
