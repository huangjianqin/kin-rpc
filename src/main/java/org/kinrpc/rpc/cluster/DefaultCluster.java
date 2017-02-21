package org.kinrpc.rpc.cluster;

import org.apache.log4j.Logger;
import org.kinrpc.registry.zookeeper.ZookeeperRegistry;
import org.kinrpc.rpc.cluster.loadbalance.LoadBalance;
import org.kinrpc.rpc.cluster.loadbalance.RoundRobinLoadBalance;
import org.kinrpc.rpc.cluster.router.Router;
import org.kinrpc.rpc.cluster.router.SimpleRouter;
import org.kinrpc.rpc.invoker.ReferenceInvoker;

import java.util.List;

/**
 * Created by 健勤 on 2017/2/15.
 */
public class DefaultCluster implements Cluster {
    private static final Logger log = Logger.getLogger(DefaultCluster.class);

    //代表某service的所有ReferenceInvoker
    private final Directory directory;
    private final Router router;
    private final LoadBalance loadBalance;

    public DefaultCluster(ZookeeperRegistry zookeeperRegistry, Class<?> interfaceClass) {
        this.directory = new ZookeeperDirectory(zookeeperRegistry, interfaceClass);
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
        if(checkState()){
            List<ReferenceInvoker> availableInvokers = directory.list();
            List<ReferenceInvoker> filtedInvokers = router.router(availableInvokers);
            ReferenceInvoker realCalledInvoker = loadBalance.loadBalance(filtedInvokers);

            log.info("real invoker(" + realCalledInvoker.getAddress() + ")");
            return  realCalledInvoker;
        }

        return null;
    }

    public void shutdown(){
        directory.destroy();
    }

    protected boolean checkState(){
        if(directory != null && router != null && loadBalance != null){
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
