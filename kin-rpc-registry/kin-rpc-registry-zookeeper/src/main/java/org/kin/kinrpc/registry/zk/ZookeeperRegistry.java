package org.kin.kinrpc.registry.zk;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.*;
import org.kin.framework.cache.ReferenceCountedCache;
import org.kin.framework.concurrent.SimpleThreadFactory;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.kinrpc.ServiceInstance;
import org.kin.kinrpc.common.Url;
import org.kin.kinrpc.config.RegistryConfig;
import org.kin.kinrpc.config.ServerConfig;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.registry.AbstractRegistry;
import org.kin.kinrpc.registry.RegistryHelper;
import org.kin.kinrpc.registry.directory.DefaultDirectory;
import org.kin.kinrpc.registry.directory.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 以zookeeper为注册中心, 实时监听服务状态变化, 并更新可用服务invoker
 *
 * @author huangjianqin
 * @date 2019/7/2
 */
public final class ZookeeperRegistry extends AbstractRegistry {
    private static final Logger log = LoggerFactory.getLogger(ZookeeperRegistry.class);
    /** zk注册中心root path */
    private static final String ROOT = "kinrpc";
    /** zk path 分隔符 */
    private static final String PATH_SEPARATOR = "/";

    /** zk注册中心root path */
    private static String root() {
        return PATH_SEPARATOR + ROOT;
    }

    /** zk注册中心服务root path */
    private static String getPath(String service) {
        return root() + PATH_SEPARATOR + service;
    }

    /** zk注册中心服务地址path */
    private static String getPath(String service, String address) {
        return getPath(service) + PATH_SEPARATOR + address;
    }

    /** zk地址 */
    private String connectAddress;
    /** curator client */
    private CuratorFramework client;
    /**
     * {@link Directory}实例缓存
     * key -> 服务唯一标识, value -> {@link Directory}实例
     */
    private final ReferenceCountedCache<String, Directory> directoryCache = new ReferenceCountedCache<>((k, v) -> v.destroy());

    public ZookeeperRegistry(RegistryConfig config) {
        super(config);
    }

    /**
     * 返回zk连接地址
     *
     * @return zk连接地址
     */
    private String connectAddress() {
        if (Objects.isNull(connectAddress)) {
            connectAddress = config.replaceSeparator(",");
        }
        return connectAddress;
    }

    /**
     * 返回zk session timeout
     *
     * @return zk session timeout
     */
    private int sessionTimeout() {
        return config.attachment(ZKConstants.SESSION_TIMEOUT_KEY, 3000);
    }

    @Override
    public void init() {
        //同步创建zk client, 原生api是异步的
        //RetryNTimes  RetryOneTime  RetryForever  RetryUntilElapsed
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        client = CuratorFrameworkFactory
                .builder()
                .connectString(connectAddress())
                .sessionTimeoutMs(sessionTimeout())
                .retryPolicy(retryPolicy)
                .threadFactory(new SimpleThreadFactory("curator"))
                //根节点会多出一个以命名空间名称所命名的节点
                .namespace(ROOT)
                .build();
        client.getConnectionStateListenable().addListener((curatorFramework, connectionState) -> {
            if (ConnectionState.CONNECTED.equals(connectionState)) {
                log.info("zookeeper registry(address={}) created", connectAddress());
            } else if (ConnectionState.RECONNECTED.equals(connectionState)) {
                log.info("zookeeper registry(address={}) reconnected", connectAddress());
                //重连时重新订阅
                for (Directory directory : directoryCache.values()) {
                    watch(directory);
                }
            } else if (ConnectionState.LOST.equals(connectionState)) {
                log.info("zookeeper registry(address={}) session expired", connectAddress());
            } else if (ConnectionState.SUSPENDED.equals(connectionState)) {
                log.error("zookeeper registry(address={}) session suspend", connectAddress());
            }
        });

        client.start();
    }

    /**
     * 创建zk node
     *
     * @param path zk path
     * @param data zk node data
     */
    private void createZNode(String path, byte[] data) {
        try {
            client.create()
                    //递归创建
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.PERSISTENT)
                    .withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE)
                    .forPath(path, data);
            if (log.isDebugEnabled()) {
                log.debug("create persistent znode(path='{}') success", path);
            }
        } catch (Exception e) {
            log.error("create persistent znode(path='{}') fail", path, e);
        }
    }

    /**
     * 删除zk node
     *
     * @param path zk path
     */
    private void deleteZNode(String path) {
        try {
            client.delete()
                    //如果删除失败, 那么在后台一直重试, 直到成功
                    .guaranteed()
                    //如果没有child, 则递归删除
                    .deletingChildrenIfNeeded()
                    .forPath(path);
            if (log.isDebugEnabled()) {
                log.debug("delete znode(path='{}') success", path);
            }
        } catch (Exception e) {
            log.error("delete znode(path='{}') fail", path, e);
        }
    }

    /**
     * 尝试删除zk node
     *
     * @param path zk path
     */
    private void tryDeleteZNode(String path) {
        try {
            client.delete().forPath(path);
            if (log.isDebugEnabled()) {
                log.debug("delete znode(path='{}') success", path);
            }
        } catch (Exception e) {
            log.error("delete znode(path='{}') fail", path, e);
        }
    }

    @Override
    public void register(ServiceConfig<?> serviceConfig) {
        String service = serviceConfig.service();
        log.info("zookeeper registry(address={}) register service '{}' ", connectAddress(), service);

        for (ServerConfig serverConfig : serviceConfig.getServers()) {
            Url url = RegistryHelper.toUrl(serviceConfig, serverConfig);
            createZNode(getPath(service, url.getAddress()), url.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public void unregister(ServiceConfig<?> serviceConfig) {
        String service = serviceConfig.service();
        log.info("zookeeper registry(address={}) unregister service '{}' ", connectAddress(), service);

        for (ServerConfig serverConfig : serviceConfig.getServers()) {
            Url url = RegistryHelper.toUrl(serviceConfig, serverConfig);
            deleteZNode(getPath(service, url.getAddress()));
            tryDeleteZNode(getPath(service));
        }
    }

    @Override
    public Directory subscribe(String service) {
        if (log.isDebugEnabled()) {
            log.debug("reference subscribe service '{}' on zookeeper registry(address={})", service, connectAddress());
        }

        return directoryCache.get(service, () -> {
            DefaultDirectory iDirectory = new DefaultDirectory(service);
            watch(iDirectory);
            return iDirectory;
        });
    }

    @Override
    public void unsubscribe(String service) {
        if (log.isDebugEnabled()) {
            log.debug("reference unsubscribe service '{}' on zookeeper registry(address={})", service, connectAddress);
        }
        directoryCache.release(service);
    }

    /**
     * 监听服务path及其childs变化
     */
    private void watch(Directory directory) {
        watchServiceNode(directory);
        watchServiceNodeChilds(directory);
    }

    /**
     * 监听服务root path
     */
    private void watchServiceNode(Directory directory) {
        try {
            client.checkExists().usingWatcher((Watcher) (WatchedEvent event) -> {
                //service root path created
                if (event.getType() == Watcher.Event.EventType.NodeCreated) {
                    if (log.isDebugEnabled()) {
                        log.debug("node '{}' created", event.getPath());
                    }
                    watch(directory);
                }

                if (event.getType() == Watcher.Event.EventType.NodeDeleted) {
                    //service root path deleted
                    if (log.isDebugEnabled()) {
                        log.debug("node '{}' deleted", event.getPath());
                    }
                    directory.discover(Collections.emptyList());
                    //监控服务节点即可, 等待服务重新注册
                    watchServiceNode(directory);
                }
            }).forPath(getPath(directory.service()));
        } catch (KeeperException e) {
            //服务还未注册或者因异常取消注册
            if (e.code().equals(KeeperException.Code.NONODE)) {
                //等待一段时间
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e1) {
                    //ignore
                }
                //尝试重新订阅服务
                watch(directory);
            } else {
                ExceptionUtils.throwExt(e);
            }
        } catch (Exception e) {
            ExceptionUtils.throwExt(e);
        }
    }

    /**
     * 监听服务root path下child node
     */
    private void watchServiceNodeChilds(Directory directory) {
        try {
            List<String> childPaths = client.getChildren().usingWatcher(
                    (Watcher) (WatchedEvent event) -> {
                        if (event.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                            if (log.isDebugEnabled()) {
                                log.debug("node '{}' childs changed", event.getPath());
                            }
                            watchServiceNodeChilds(directory);
                        }
                    }).forPath(getPath(directory.service()));
            //获取child nodes的data
            List<String> urls = new ArrayList<>(childPaths.size());
            for (String path : childPaths) {
                byte[] data = client.getData().forPath(getPath(directory.service(), path));
                if (Objects.nonNull(data) && data.length > 0) {
                    urls.add(new String(data, StandardCharsets.UTF_8));
                }
            }

            List<ServiceInstance> serviceInstances = urls.stream().map(RegistryHelper::parseUrl).collect(Collectors.toList());
            directory.discover(serviceInstances);
        } catch (InterruptedException e) {
            //ignore
        } catch (Exception e) {
            ExceptionUtils.throwExt(e);
        }
    }

    @Override
    public void destroy() {
        if (Objects.isNull(client)) {
            return;
        }

        client.close();
        directoryCache.clear();
        log.info("zookeeper registry(address={}) destroyed", connectAddress());
    }
}
