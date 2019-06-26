package org.kin.kinrpc.registry.zookeeper;

import com.google.common.net.HostAndPort;
import org.apache.zookeeper.*;
import org.kin.framework.utils.HttpUtils;
import org.kin.kinrpc.registry.AbstractRegistry;
import org.kin.kinrpc.registry.Directory;
import org.kin.kinrpc.registry.RegistryConstants;
import org.kin.kinrpc.rpc.serializer.SerializerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.zip.DataFormatException;

/**
 * Created by 健勤 on 2016/10/10.
 * <p>
 * zookeeper作为注册中心, 操作zookeeper node
 */
public class ZookeeperRegistry extends AbstractRegistry{
    private static final Logger log = LoggerFactory.getLogger("registry");

    protected final HostAndPort address;
    protected String password;

    private ZooKeeper zooKeeper;
    private int sessionTimeOut;
    private SerializerType serializerType;

    public ZookeeperRegistry(String address, String password, int sessionTimeOut, SerializerType serializerType) {
        this.address = HostAndPort.fromString(address);
        this.password = password;
        this.sessionTimeOut = sessionTimeOut;
        this.serializerType = serializerType;
    }

    @Override
    public void connect() throws DataFormatException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            this.zooKeeper = new ZooKeeper(address.getHost(), this.sessionTimeOut, new Watcher() {
                @Override
                public void process(WatchedEvent watchedEvent) {
                    if (watchedEvent.getState() == Event.KeeperState.SyncConnected) {
                        countDownLatch.countDown();
                        log.info("zookeeper registry created");
                    } else if (watchedEvent.getState() == Event.KeeperState.Expired) {
                        throw new RuntimeException("connect to zookeeper server timeout(" + sessionTimeOut + ")");
                    } else if (watchedEvent.getState() == Event.KeeperState.Disconnected) {
                        throw new RuntimeException("disconnect to zookeeper server");
                    }
                }
            });
        } catch (IOException e) {
            log.error("zookeeper client connect error" + System.lineSeparator() + "{}", e);
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
        }

        //一些预处理
        initRootZNode();
    }

    private void initRootZNode() {
        //如果没有创建根节点
        notExistAndCreate(RegistryConstants.REGISTRY_ROOT, null);
    }

    private void createZNode(String path, byte[] data) {
        try {
            this.zooKeeper.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            log.debug("create persistent znode(data= '{}') successfully>>> {}", data, path);
        } catch (KeeperException | InterruptedException e) {
            log.error("", e);
        }
    }

    private void deleteZNode(String path) {
        try {
            this.zooKeeper.delete(path, -1);
            log.debug("delete znode successfully>>> " + path);
        } catch (KeeperException | InterruptedException e) {
            log.error("", e);
        }
    }

    private void notExistAndCreate(String path, byte[] data) {
        try {
            if (this.zooKeeper.exists(path, false) == null) {
                log.debug("znode: " + path + " >>> not exist. now create");
                createZNode(path, data);
            }
        } catch (KeeperException | InterruptedException e) {
            log.error("", e);
        }
    }

    private void deleteIfNoChilds(String path) {
        try {
            List<String> childs = this.zooKeeper.getChildren(path, false);
            if (childs == null || childs.size() <= 0) {
                deleteZNode(path);
            }
        } catch (KeeperException | InterruptedException e) {
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

        String servicePath = RegistryConstants.getPath(serviceName);
        String serviceHostPath = RegistryConstants.getPath(serviceName, address);

        notExistAndCreate(servicePath, null);
        createZNode(serviceHostPath, null);
    }

    @Override
    public void unRegister(String serviceName, String host, int port) {
        log.info("provider unregister service '{}' " + ">>> zookeeper registry({})", serviceName, getAddress());
        String address = host + ":" + port;

        String servicePath = RegistryConstants.getPath(serviceName);
        String serviceHostPath = RegistryConstants.getPath(serviceName, address);

        deleteZNode(serviceHostPath);
        deleteIfNoChilds(servicePath);
    }

    @Override
    public Directory subscribe(String serviceName, int connectTimeout) {
        log.info("consumer subscribe service '{}' " + ">>> zookeeper registry({})", serviceName, getAddress());
        return new ZookeeperDirectory(serviceName, connectTimeout, this, serializerType);
    }

    @Override
    public void destroy() {
        if (zooKeeper != null) {
            try {
                log.info("zookeeper registry destroying...");
                zooKeeper.close();
            } catch (InterruptedException e) {
                log.error("", e);
            }
        }
        log.info("zookeeper registry destroy successfully");
    }

    //setter && getter
    public int getSessionTimeOut() {
        return sessionTimeOut;
    }

    public ZooKeeper getConnection() {
        return this.zooKeeper;
    }

    public HostAndPort getAddress() {
        return address;
    }

    public String getPassword() {
        return password;
    }
}
