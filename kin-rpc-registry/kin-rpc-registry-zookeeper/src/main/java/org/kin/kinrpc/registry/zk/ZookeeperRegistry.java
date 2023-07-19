package org.kin.kinrpc.registry.zk;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.*;
import org.kin.framework.concurrent.SimpleThreadFactory;
import org.kin.kinrpc.ApplicationInstance;
import org.kin.kinrpc.DefaultApplicationInstance;
import org.kin.kinrpc.ReferenceContext;
import org.kin.kinrpc.ServiceMetadataConstants;
import org.kin.kinrpc.common.Url;
import org.kin.kinrpc.config.RegistryConfig;
import org.kin.kinrpc.config.ServerConfig;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.registry.DiscoveryRegistry;
import org.kin.kinrpc.registry.RegistryHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 以zookeeper为注册中心, 实时监听应用实例状态变化, 并更新可用{@link org.kin.kinrpc.ReferenceInvoker}实例
 *
 * @author huangjianqin
 * @date 2019/7/2
 */
public final class ZookeeperRegistry extends DiscoveryRegistry {
    private static final Logger log = LoggerFactory.getLogger(ZookeeperRegistry.class);
    /** zk注册中心root path */
    private static final String ROOT = "kinrpc";
    /** zk path 分隔符 */
    private static final String PATH_SEPARATOR = "/";

    /** zk注册中心root path */
    private static String root() {
        return PATH_SEPARATOR + ROOT;
    }

    /** zk注册中心{@link #ROOT}/appGroup path */
    private static String getPath(String group) {
        return PATH_SEPARATOR + group;
    }

    /** zk注册中心应用path */
    private static String getPath(String group, String appName) {
        return getPath(group) + PATH_SEPARATOR + appName;
    }

    /** zk注册中心应用path */
    private static String getPath(String group, String appName, String instance) {
        return getPath(group) + PATH_SEPARATOR + appName + PATH_SEPARATOR + instance;
    }

    /** zk地址 */
    private String connectAddress;
    /** curator client */
    private CuratorFramework client;


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
                watchAppGroupNode(config.getGroup());
            } else if (ConnectionState.RECONNECTED.equals(connectionState)) {
                log.info("zookeeper registry(address={}) reconnected", connectAddress());
                watchAppGroupNode(config.getGroup());
                for (String appName : getWatchingAppNames()) {
                    watchAppNodeChildren(appName);
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
     */
    private void createZNode(String path) {
        try {
            client.create()
                    //递归创建
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.PERSISTENT)
                    .withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE)
                    .forPath(path);
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
    public void doRegister(ServiceConfig<?> serviceConfig) {
        String appName = serviceConfig.getApp().getAppName();
        for (ServerConfig serverConfig : serviceConfig.getServers()) {
            Url url = RegistryHelper.toUrl(serviceConfig, serverConfig);
            createZNode(getPath(config.getGroup(), appName, Url.encode(url.toString())));
        }
    }

    @Override
    public void doUnregister(ServiceConfig<?> serviceConfig) {
        String appName = serviceConfig.getApp().getAppName();
        for (ServerConfig serverConfig : serviceConfig.getServers()) {
            Url url = RegistryHelper.toUrl(serviceConfig, serverConfig);
            deleteZNode(getPath(config.getGroup(), appName, Url.encode(url.toString())));
        }
        tryDeleteZNode(getPath(config.getGroup(), appName));
    }

    @Override
    protected void watch(Set<String> appNames) {
        for (String appName : appNames) {
            watchAppNodeChildren(appName);
        }
    }

    /**
     * 监听应用组节点
     */
    private void watchAppGroupNode(String group) {
        if (isTerminated()) {
            return;
        }

        try {
            client.checkExists().usingWatcher((Watcher) (WatchedEvent event) -> {
                //application group path created
                if (event.getType() == Watcher.Event.EventType.NodeCreated) {
                    if (log.isDebugEnabled()) {
                        log.debug("node '{}' created", event.getPath());
                    }
                    if (isWatching(event.getPath())) {
                        watchAppNodeChildren(event.getPath());
                    }
                    watchAppGroupNode(group);
                }

                if (event.getType() == Watcher.Event.EventType.NodeDeleted) {
                    //application group root path deleted
                    if (log.isDebugEnabled()) {
                        log.debug("node '{}' deleted", event.getPath());
                    }
                    onAppInstancesChanged(event.getPath(), Collections.emptyList());
                    //监控应用组节点即可, 等待应用实例重新注册
                    watchAppGroupNode(group);
                }
            }).forPath(getPath(group));
        } catch (KeeperException e) {
            //应用还未注册或者因异常取消注册
            if (e.code().equals(KeeperException.Code.NONODE)) {
                //等待一段时间
                ReferenceContext.DISCOVERY_SCHEDULER.schedule(() -> {
                    //尝试重新监听应用组节点
                    watchAppGroupNode(group);
                }, 3, TimeUnit.SECONDS);
            } else {
                log.error("{} watch application group node '{}' error", getName(), group, e);
            }
        } catch (Exception e) {
            log.error("{} watch application group node '{}' error", getName(), group, e);
        }
    }

    /**
     * 监听应用节点下子节点
     */
    private void watchAppNodeChildren(String appName) {
        if (isTerminated()) {
            return;
        }

        try {
            List<String> childPaths = client.getChildren().usingWatcher(
                    (Watcher) (WatchedEvent event) -> {
                        if (event.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                            if (log.isDebugEnabled()) {
                                log.debug("node '{}' childs changed", event.getPath());
                            }
                            watchAppNodeChildren(appName);
                        }
                    }).forPath(getPath(config.getGroup(), appName));
            //并发获取zk node data
            List<ApplicationInstance> appInstances = new ArrayList<>(childPaths.size());
            for (String childPath : childPaths) {
                Url url = Url.of(Url.decode(childPath));
                Map<String, String> metadata = url.getParams();
                metadata.put(ServiceMetadataConstants.SCHEMA_KEY, url.getProtocol());
                appInstances.add(new DefaultApplicationInstance(appName, url.getHost(), url.getPort(), metadata));
            }

            onAppInstancesChanged(appName, appInstances);
        } catch (InterruptedException e) {
            //ignore
        } catch (KeeperException e) {
            ReferenceContext.DISCOVERY_SCHEDULER.schedule(() -> {
                //尝试重新监听应用节点下子节点
                watchAppNodeChildren(appName);
            }, 3, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("{} watch application node '{}' error", getName(), appName, e);
        }
    }

    @Override
    public void doDestroy() {
        if (Objects.isNull(client)) {
            return;
        }

        client.close();
    }
}
