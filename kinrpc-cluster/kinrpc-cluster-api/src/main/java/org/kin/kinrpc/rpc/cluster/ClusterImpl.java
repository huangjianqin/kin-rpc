package org.kin.kinrpc.rpc.cluster;

import org.kin.kinrpc.registry.Directory;
import org.kin.kinrpc.registry.Registry;
import org.kin.kinrpc.rpc.cluster.loadbalance.LoadBalance;
import org.kin.kinrpc.rpc.cluster.loadbalance.impl.RoundRobinLoadBalance;
import org.kin.kinrpc.rpc.cluster.router.Router;
import org.kin.kinrpc.rpc.cluster.router.impl.SimpleRouter;
import org.kin.kinrpc.rpc.invoker.AbstractReferenceInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by 健勤 on 2017/2/15.
 */
class ClusterImpl implements Cluster {
    private static final Logger log = LoggerFactory.getLogger("cluster");

    //代表某service的所有ReferenceInvoker
    private final Directory directory;
    private final Router router;
    private final LoadBalance loadBalance;

    public ClusterImpl(Registry registry, String serviceName, int connectTimeout) {
        this.directory = registry.subscribe(serviceName, connectTimeout);
        this.router = new SimpleRouter();
        this.loadBalance = new RoundRobinLoadBalance();
    }

    public ClusterImpl(Directory directory, Router router, LoadBalance loadBalance) {
        this.directory = directory;
        this.router = router;
        this.loadBalance = loadBalance;
    }

    @Override
    public AbstractReferenceInvoker get() {
        log.debug("get one reference invoker from cluster");
        if (checkState()) {
            List<AbstractReferenceInvoker> availableInvokers = directory.list();
            List<AbstractReferenceInvoker> routeredInvokers = router.router(availableInvokers);
            AbstractReferenceInvoker loadbalancedInvoker = loadBalance.loadBalance(routeredInvokers);

            log.debug("real invoker(" + loadbalancedInvoker.getAddress() + ")");
            return loadbalancedInvoker;
        }

        return null;
    }

    @Override
    public void shutdown() {
        directory.destroy();
    }

    protected boolean checkState() {
        if (directory != null && router != null && loadBalance != null) {
            return true;
        }

        return false;
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
