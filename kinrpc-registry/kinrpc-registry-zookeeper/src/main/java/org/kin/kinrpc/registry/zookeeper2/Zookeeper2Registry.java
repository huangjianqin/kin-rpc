package org.kin.kinrpc.registry.zookeeper2;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.kin.framework.utils.HttpUtils;
import org.kin.kinrpc.registry.AbstractRegistry;
import org.kin.kinrpc.registry.Directory;
import org.kin.kinrpc.registry.common.RegistryConstants;
import org.kin.kinrpc.registry.zookeeper.ZookeeperDirectory;
import org.kin.kinrpc.rpc.serializer.SerializerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.zip.DataFormatException;

/**
 * @author huangjianqin
 * @date 2019/7/2
 */
public class Zookeeper2Registry extends AbstractRegistry{
    private static final Logger log = LoggerFactory.getLogger("registry");

    protected String address;

    private CuratorFramework client;
    private int sessionTimeOut;
    private SerializerType serializerType;

    public Zookeeper2Registry(String address, int sessionTimeOut, SerializerType serializerType) {
        this.address = address;
        this.sessionTimeOut = sessionTimeOut;
        this.serializerType = serializerType;
    }

    @Override
    public void connect() throws DataFormatException {
        //同步创建zk client，原生api是异步的
        //RetryNTimes  RetryOneTime  RetryForever  RetryUntilElapsed
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(2000, 5);
        client = CuratorFrameworkFactory
                .builder()
                .connectString(address)
                .sessionTimeoutMs(sessionTimeOut)
                .retryPolicy(retryPolicy)
                //根节点会多出一个以命名空间名称所命名的节点
                .namespace(RegistryConstants.REGISTRY_ROOT)
                .build();

        client.start();
        log.info("zookeeper registry created");
    }

    private void createZNode(String path, byte[] data) {
        try {
            client.create()
                    //递归创建
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.PERSISTENT)
                    .withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE)
                    .forPath(path, data);
            log.debug("create persistent znode(data= '{}') successfully>>> {}", data, path);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    private void deleteZNode(String path) {
        try {
            client.delete()
                    //如果删除失败, 那么在后台还是会继续删除, 直到成功
                    .guaranteed()
                    //递归删除
                    .deletingChildrenIfNeeded()
                    .forPath(path);
            log.debug("delete znode successfully>>> " + path);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    private void deleteZNodeUnguaranteed(String path) {
        try {
            client.delete().forPath(path);
            log.debug("delete znode successfully>>> " + path);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    @Override
    public void register(String serviceName, String host, int port) throws DataFormatException {
        log.info("provider register service '{}' " + ">>> zookeeper registry({})", serviceName, getAddress());
        String address = host + ":" + port;

        if (!HttpUtils.checkHostPort(address)) {
            throw new DataFormatException("zookeeper registry address('" + address + "') format error");
        }

        createZNode(RegistryConstants.getPath(serviceName, address), null);
    }

    @Override
    public void unRegister(String serviceName, String host, int port) {
        log.info("provider unregister service '{}' " + ">>> zookeeper registry({})", serviceName, getAddress());
        String address = host + ":" + port;

        deleteZNode(RegistryConstants.getPath(serviceName, address));
        deleteZNodeUnguaranteed(RegistryConstants.getPath(serviceName));
    }

    @Override
    public Directory subscribe(String serviceName, int connectTimeout) {
        log.info("reference subscribe service '{}' " + ">>> zookeeper registry({})", serviceName, getAddress());
        Directory directory = new ZookeeperDirectory(serviceName, connectTimeout, serializerType);
        watch(directory);
        return directory;
    }

    private void watch(Directory directory){
        try {
            List<String> znodeList = client.getChildren()
                    .usingWatcher((Watcher) (WatchedEvent watchedEvent) -> {
                        if (watchedEvent.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                            log.info("service '" + directory.getServiceName() + "' node changed");
                            watch(directory);
                        }
                    }).forPath(RegistryConstants.getPath(directory.getServiceName()));
            directory.discover(znodeList);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    @Override
    public void destroy() {
        if (client != null) {
            client.close();
            log.info("zookeeper registry destroy successfully");
        }

    }

    //setter && getter
    public int getSessionTimeOut() {
        return sessionTimeOut;
    }

    public String getAddress() {
        return address;
    }
}
