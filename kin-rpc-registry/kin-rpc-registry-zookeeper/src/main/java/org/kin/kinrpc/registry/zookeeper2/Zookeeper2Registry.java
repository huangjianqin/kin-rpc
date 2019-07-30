package org.kin.kinrpc.registry.zookeeper2;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.*;
import org.kin.framework.concurrent.SimpleThreadFactory;
import org.kin.framework.utils.NetUtils;
import org.kin.kinrpc.registry.AbstractRegistry;
import org.kin.kinrpc.registry.Directory;
import org.kin.kinrpc.registry.common.RegistryConstants;
import org.kin.kinrpc.registry.exception.AddressFormatErrorException;
import org.kin.kinrpc.registry.zookeeper.ZookeeperDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2019/7/2
 */
public class Zookeeper2Registry extends AbstractRegistry{
    private static final Logger log = LoggerFactory.getLogger(Zookeeper2Registry.class);

    protected String address;

    private CuratorFramework client;
    private int sessionTimeOut;
    private String serializerType;

    public Zookeeper2Registry(String address, int sessionTimeOut, String serializerType) {
        this.address = address;
        this.sessionTimeOut = sessionTimeOut;
        this.serializerType = serializerType;
    }

    @Override
    public void connect(){
        //同步创建zk client，原生api是异步的
        //RetryNTimes  RetryOneTime  RetryForever  RetryUntilElapsed
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(200, 5);
        client = CuratorFrameworkFactory
                .builder()
                .connectString(address)
                .sessionTimeoutMs(sessionTimeOut)
                .retryPolicy(retryPolicy)
                .threadFactory(new SimpleThreadFactory("curator"))
                //根节点会多出一个以命名空间名称所命名的节点
//                .namespace(RegistryConstants.REGISTRY_ROOT)
                .build();
        client.getConnectionStateListenable().addListener((curatorFramework, connectionState) -> {
            if(ConnectionState.CONNECTED.equals(connectionState)){
                log.info("zookeeper registry created");
            }else if(ConnectionState.RECONNECTED.equals(connectionState)){
                log.info("zookeeper registry reconnected");
                //重连时重新订阅
                for(Directory directory: DIRECTORY_CACHE.asMap().values()){
                    watch(directory);
                }
            }else if(ConnectionState.LOST.equals(connectionState)){
                log.info("disconnect to zookeeper server");
                handleConnectError();
            }else if(ConnectionState.SUSPENDED.equals(connectionState)){
                log.error("connect to zookeeper server timeout '{}'", sessionTimeOut);
                handleConnectError();
            }
        });

        client.start();
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
            log.error(e.getMessage(), e);
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
            log.error(e.getMessage(), e);
        }
    }

    private void deleteZNodeUnguaranteed(String path) {
        try {
            client.delete().forPath(path);
            log.debug("delete znode successfully>>> " + path);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void register(String serviceName, String host, int port){
        log.info("provider register service '{}' ", serviceName);
        String address = host + ":" + port;

        if (!NetUtils.checkHostPort(address)) {
            throw new AddressFormatErrorException(address);
        }

        createZNode(RegistryConstants.getPath(serviceName, address), null);
    }

    @Override
    public void unRegister(String serviceName, String host, int port) {
        log.info("provider unregister service '{}' ", serviceName);
        String address = host + ":" + port;

        deleteZNode(RegistryConstants.getPath(serviceName, address));
        deleteZNodeUnguaranteed(RegistryConstants.getPath(serviceName));
    }

    @Override
    public Directory subscribe(String serviceName, int connectTimeout) {
        log.info("reference subscribe service '{}' ", serviceName);
        Directory directory = new ZookeeperDirectory(serviceName, connectTimeout, serializerType);
        watch(directory);
        DIRECTORY_CACHE.put(serviceName, directory);
        return directory;
    }

    @Override
    public void unSubscribe(String serviceName) {
        log.info("reference unsubscribe service '{}' ", serviceName);
        Directory directory = DIRECTORY_CACHE.getIfPresent(serviceName);
        if(directory != null){
            directory.destroy();
        }
        DIRECTORY_CACHE.invalidate(serviceName);
    }

    private void watch(Directory directory){
        watchServiveNode(directory);
        watchServiveNodeChilds(directory);
    }

    /**
     * 监听服务根节点
     */
    private void watchServiveNode(Directory directory){
        try {
            client.checkExists().usingWatcher((Watcher) (WatchedEvent watchedEvent) -> {
                if (watchedEvent.getType() == Watcher.Event.EventType.NodeCreated) {
                    log.info("service '" + directory.getServiceName() + "' node created");
                    watch(directory);
                }

                if (watchedEvent.getType() == Watcher.Event.EventType.NodeDeleted) {
                    log.info("service '" + directory.getServiceName() + "' node deleted");
                    directory.discover(Collections.emptyList());
                    //监控服务节点即可, 等待服务重新注册
                    watchServiveNode(directory);
                }
            }).forPath(RegistryConstants.getPath(directory.getServiceName()));
        } catch (KeeperException e) {
            //服务还未注册或者因异常取消注册
            if (e.code().equals(KeeperException.Code.NONODE)) {
                //等待一段时间
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e1) {
                }
                //尝试重新订阅服务
                watch(directory);
            }
            else{
                log.error(e.getMessage(), e);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 监听服务子节点
     */
    private void watchServiveNodeChilds(Directory directory){
        try {
            List<String> addresses = client.getChildren().usingWatcher(
                    (Watcher) (WatchedEvent watchedEvent) -> {
                        if (watchedEvent.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                            log.info("service '" + directory.getServiceName() + "' node childs changed");
                            watchServiveNodeChilds(directory);
                        }
                    }).forPath(RegistryConstants.getPath(directory.getServiceName()));
            directory.discover(addresses);
        } catch (KeeperException e) {
            log.error(e.getMessage(), e);
        } catch (InterruptedException e) {

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void handleConnectError(){
        //断连时, 让所有directory持有的invoker失效
        for(Directory directory: DIRECTORY_CACHE.asMap().values()){
            directory.discover(Collections.emptyList());
        }
    }

    @Override
    public void destroy() {
        if (client != null) {
            client.close();
            for(Directory directory: DIRECTORY_CACHE.asMap().values()){
                directory.destroy();
            }
            DIRECTORY_CACHE.invalidateAll();
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
