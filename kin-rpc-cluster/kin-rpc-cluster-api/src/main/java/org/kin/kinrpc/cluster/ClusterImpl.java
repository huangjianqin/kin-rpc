package org.kin.kinrpc.cluster;

import com.google.common.net.HostAndPort;
import org.kin.kinrpc.cluster.loadbalance.LoadBalance;
import org.kin.kinrpc.cluster.router.Router;
import org.kin.kinrpc.registry.Directory;
import org.kin.kinrpc.registry.Registry;
import org.kin.kinrpc.rpc.AsyncInvoker;
import org.kin.kinrpc.rpc.common.Url;
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

    /** 该{@link Cluster}对应的reference信息 */
    private final Url url;
    /** 发现service的所有{@link org.kin.kinrpc.rpc.Invoker} */
    private final Directory directory;
    /** 定义路由策略 */
    private final Router router;
    /** 定义负载均衡策略 */
    private final LoadBalance loadBalance;

    public ClusterImpl(Registry registry, Url url, Router router, LoadBalance loadBalance) {
        this(url, registry.subscribe(url.getServiceKey()), router, loadBalance);
    }

    public ClusterImpl(Url url, Directory directory, Router router, LoadBalance loadBalance) {
        this.url = url;
        this.directory = directory;
        this.router = router;
        this.loadBalance = loadBalance;
    }

    @Override
    public AsyncInvoker<T> get(String method, Object[] params, Collection<HostAndPort> excludes) {
        log.debug("get one reference invoker from cluster");
        //1. list invokers
        List<AsyncInvoker> availableInvokers = directory.list();
        //过滤掉单次请求曾经fail的service 访问地址
        availableInvokers = availableInvokers.stream().filter(invoker -> !excludes.contains(HostAndPort.fromString(invoker.url().getAddress())))
                .collect(Collectors.toList());
        //2. route
        List<AsyncInvoker> routeredInvokers = router. router(availableInvokers);
        //3. load balance
        AsyncInvoker loadbalancedInvoker = loadBalance.loadBalance(url.getServiceKey(), method, params, routeredInvokers);

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
