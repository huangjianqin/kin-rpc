package org.kin.kinrpc.registry.zookeeper;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.*;
import org.kin.framework.concurrent.SimpleThreadFactory;
import org.kin.kinrpc.registry.AbstractRegistry;
import org.kin.kinrpc.registry.Directory;
import org.kin.kinrpc.registry.common.RegistryConstants;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 以zookeeper为注册中心, 实时监听服务状态变化, 并更新可调用服务invoker
 * 无效invoker由zookeeper注册中心控制, 所以可能会存在list有无效invoker(zookeeper没有及时更新到)
 *
 * @author huangjianqin
 * @date 2019/7/2
 */
public final class ZookeeperRegistry extends AbstractRegistry {
    private static final Logger log = LoggerFactory.getLogger(ZookeeperRegistry.class);

    private final String address;

    private CuratorFramework client;
    private final long sessionTimeout;

    public ZookeeperRegistry(Url url) {
        super(url);
        this.address = url.getParam(Constants.REGISTRY_URL_KEY);
        this.sessionTimeout = Long.parseLong(url.getParam(Constants.SESSION_TIMEOUT_KEY));
    }

    @Override
    public void connect() {
        //同步创建zk client，原生api是异步的
        //RetryNTimes  RetryOneTime  RetryForever  RetryUntilElapsed
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(200, 5);
        client = CuratorFrameworkFactory
                .builder()
                .connectString(address)
                .sessionTimeoutMs((int) sessionTimeout)
                .retryPolicy(retryPolicy)
                .threadFactory(new SimpleThreadFactory("curator"))
                //根节点会多出一个以命名空间名称所命名的节点
//                .namespace(RegistryConstants.REGISTRY_ROOT)
                .build();
        client.getConnectionStateListenable().addListener((curatorFramework, connectionState) -> {
            if (ConnectionState.CONNECTED.equals(connectionState)) {
                log.info("zookeeper registry created");
            } else if (ConnectionState.RECONNECTED.equals(connectionState)) {
                log.info("zookeeper registry reconnected");
                //重连时重新订阅
                for (Directory directory : directoryCache.asMap().values()) {
                    watch(directory);
                }
            } else if (ConnectionState.LOST.equals(connectionState)) {
                log.info("disconnect to zookeeper server");
                handleConnectError();
            } else if (ConnectionState.SUSPENDED.equals(connectionState)) {
                log.error("connect to zookeeper server timeout '{}'", sessionTimeout);
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
    public void register(Url url) {
        String serviceKey = url.getServiceKey();
        String address = url.getAddress();
        log.info("provider register service '{}' ", serviceKey);

        createZNode(RegistryConstants.getPath(serviceKey, address), url.str().getBytes());
    }

    @Override
    public void unRegister(Url url) {
        String serviceKey = url.getServiceKey();
        String address = url.getAddress();
        log.info("provider unregister service '{}' ", serviceKey);

        deleteZNode(RegistryConstants.getPath(serviceKey, address));
        deleteZNodeUnguaranteed(RegistryConstants.getPath(serviceKey));
    }

    @Override
    public Directory subscribe(String serviceKey) {
        log.info("reference subscribe service '{}' ", serviceKey);
        Directory directory = new Directory(serviceKey);
        watch(directory);
        directoryCache.put(serviceKey, directory);
        return directory;
    }

    @Override
    public void unSubscribe(String serviceKey) {
        log.info("reference unsubscribe service '{}' ", serviceKey);
        Directory directory = directoryCache.getIfPresent(serviceKey);
        if (directory != null) {
            directory.destroy();
        }
        directoryCache.invalidate(serviceKey);
    }

    /**
     * 监控某服务
     */
    private void watch(Directory directory) {
        watchServiveNode(directory);
        watchServiveNodeChilds(directory);
    }

    /**
     * 监听服务根节点
     */
    private void watchServiveNode(Directory directory) {
        try {
            client.checkExists().usingWatcher((Watcher) (WatchedEvent watchedEvent) -> {
                if (watchedEvent.getType() == Watcher.Event.EventType.NodeCreated) {
                    log.info("service '" + directory.getServiceName() + "' node created");
                    watch(directory);
                }

                if (watchedEvent.getType() == Watcher.Event.EventType.NodeDeleted) {
                    log.info("service '" + directory.getServiceName() + "' node deleted");
                    directory.discover(url, Collections.emptyList());
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
            } else {
                log.error(e.getMessage(), e);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 监听服务子节点
     */
    private void watchServiveNodeChilds(Directory directory) {
        try {
            //获取{root}/{serviceName}/ child nodes
            List<String> addresses = client.getChildren().usingWatcher(
                    (Watcher) (WatchedEvent watchedEvent) -> {
                        if (watchedEvent.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                            log.info("service '" + directory.getServiceName() + "' node childs changed");
                            watchServiveNodeChilds(directory);
                        }
                    }).forPath(RegistryConstants.getPath(directory.getServiceName()));
            //获取child nodes的data
            List<Url> urls = new ArrayList<>(addresses.size());
            for (String address : addresses) {
                byte[] data = client.getData().forPath(RegistryConstants.getPath(directory.getServiceName(), address));
                if (Objects.nonNull(data) && data.length > 0) {
                    urls.add(Url.of(new String(data)));
                }
            }
            directory.discover(url, urls);
        } catch (KeeperException e) {
            log.error(e.getMessage(), e);
        } catch (InterruptedException e) {

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void handleConnectError() {
        //断连时, 让所有directory持有的invoker失效
        for (Directory directory : directoryCache.asMap().values()) {
            directory.discover(url, Collections.emptyList());
        }
    }

    @Override
    public void destroy() {
        if (client != null) {
            client.close();
            for (Directory directory : directoryCache.asMap().values()) {
                directory.destroy();
            }
            directoryCache.invalidateAll();
            log.info("zookeeper registry destroy successfully");
        }

    }

    //setter && getter
    public long getSessionTimeout() {
        return sessionTimeout;
    }

    public String getAddress() {
        return address;
    }
}
