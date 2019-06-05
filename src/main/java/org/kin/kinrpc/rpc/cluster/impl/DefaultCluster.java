package org.kin.kinrpc.rpc.cluster.impl;

import org.kin.kinrpc.rpc.cluster.Cluster;
import org.kin.kinrpc.rpc.cluster.Directory;
import org.kin.kinrpc.rpc.cluster.loadbalance.LoadBalance;
import org.kin.kinrpc.rpc.cluster.loadbalance.impl.RoundRobinLoadBalance;
import org.kin.kinrpc.rpc.cluster.router.Router;
import org.kin.kinrpc.rpc.cluster.router.impl.SimpleRouter;
import org.kin.kinrpc.rpc.invoker.impl.ReferenceInvoker;
import org.kin.kinrpc.rpc.registry.zookeeper.ZookeeperRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by 健勤 on 2017/2/15.
 */
public class DefaultCluster implements Cluster {
    private static final Logger log = LoggerFactory.getLogger(DefaultCluster.class);

    //代表某service的所有ReferenceInvoker
    private final Directory directory;
    private final Router router;
    private final LoadBalance loadBalance;

    public DefaultCluster(ZookeeperRegistry zookeeperRegistry, Class<?> interfaceClass, int connectTimeout) {
        this.directory = zookeeperRegistry.subscribe(interfaceClass, connectTimeout);
        this.router = new SimpleRouter();
        this.loadBalance = new RoundRobinLoadBalance();
    }

    public DefaultCluster(Directory directory, Router router, LoadBalance loadBalance) {
        this.directory = directory;
        this.router = router;
        this.loadBalance = loadBalance;
    }

    public ReferenceInvoker get() {
        log.info("get one reference invoker from cluster");
        if (checkState()) {
            List<ReferenceInvoker> availableInvokers = directory.list();
            List<ReferenceInvoker> filtedInvokers = router.router(availableInvokers);
            ReferenceInvoker realCalledInvoker = loadBalance.loadBalance(filtedInvokers);

            log.info("real invoker(" + realCalledInvoker.getAddress() + ")");
            return realCalledInvoker;
        }

        return null;
    }

    public void shutdown() {
        directory.destroy();
    }

    protected boolean checkState() {
        if (directory != null && router != null && loadBalance != null) {
            return true;
        }

        return false;
    }

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
