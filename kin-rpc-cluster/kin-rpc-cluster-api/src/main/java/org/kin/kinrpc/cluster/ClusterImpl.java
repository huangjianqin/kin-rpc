package org.kin.kinrpc.cluster;

import com.google.common.net.HostAndPort;
import org.kin.kinrpc.cluster.loadbalance.LoadBalance;
import org.kin.kinrpc.cluster.router.Router;
import org.kin.kinrpc.registry.Directory;
import org.kin.kinrpc.registry.Registry;
import org.kin.kinrpc.rpc.AsyncInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by 健勤 on 2017/2/15.
 */
class ClusterImpl<T> implements Cluster<T> {
    private static final Logger log = LoggerFactory.getLogger(ClusterImpl.class);

    /** 代表某service的所有ReferenceInvoker */
    private final Directory directory;
    private final Router router;
    private final LoadBalance loadBalance;

    public ClusterImpl(Registry registry, String serviceKey, Router router, LoadBalance loadBalance) {
        this(registry.subscribe(serviceKey), router, loadBalance);
    }

    public ClusterImpl(Directory directory, Router router, LoadBalance loadBalance) {
        this.directory = directory;
        this.router = router;
        this.loadBalance = loadBalance;
    }

    @Override
    public AsyncInvoker<T> get(Collection<HostAndPort> excludes) {
        log.debug("get one reference invoker from cluster");
        List<AsyncInvoker> availableInvokers = directory.list();
        //过滤掉单次请求曾经fail的service 访问地址
        availableInvokers = availableInvokers.stream().filter(invoker -> !excludes.contains(HostAndPort.fromString(invoker.url().getAddress())))
                .collect(Collectors.toList());

        List<AsyncInvoker> routeredInvokers = router.router(availableInvokers);
        AsyncInvoker loadbalancedInvoker = loadBalance.loadBalance(routeredInvokers);

        if (loadbalancedInvoker != null) {
            log.debug("real invoker(" + loadbalancedInvoker.url().getAddress() + ")");
        }
        return loadbalancedInvoker;
    }

    @Override
    public void shutdown() {
    }

    //getter

    public Directory getDirectory() {
        return directory;
    }

    public Router getRouter() {
        return router;
    }

    public LoadBalance getLoadBalance() {
        return loadBalance;
    }

}
