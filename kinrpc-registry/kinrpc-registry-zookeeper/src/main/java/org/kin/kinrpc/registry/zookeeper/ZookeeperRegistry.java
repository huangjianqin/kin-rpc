package org.kin.kinrpc.registry.zookeeper;

import org.apache.zookeeper.*;
import org.kin.framework.utils.HttpUtils;
import org.kin.kinrpc.registry.AbstractRegistry;
import org.kin.kinrpc.registry.Directory;
import org.kin.kinrpc.registry.common.RegistryConstants;
import org.kin.kinrpc.registry.exception.AddressFormatErrorException;
import org.kin.kinrpc.rpc.serializer.SerializerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Created by 健勤 on 2016/10/10.
 * <p>
 * zookeeper作为注册中心, 操作zookeeper node
 */
public class ZookeeperRegistry extends AbstractRegistry{
    protected String address;

    private volatile ZooKeeper zooKeeper;
    private int sessionTimeOut;
    private SerializerType serializerType;

    public ZookeeperRegistry(String address, int sessionTimeOut, SerializerType serializerType) {
        this.address = address;
        this.sessionTimeOut = sessionTimeOut;
        this.serializerType = serializerType;
    }

    @Override
    public void connect(){
        CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            zooKeeper = new ZooKeeper(address, sessionTimeOut, new Watcher() {
                @Override
                public void process(WatchedEvent watchedEvent) {
                    if (watchedEvent.getState() == Event.KeeperState.SyncConnected) {
                        countDownLatch.countDown();
                        //首次连接Cache不会有内容
                        //重连时重新订阅
                        for(Directory directory: DIRECTORY_CACHE.asMap().values()){
                            watch(directory);
                        }
                        log.info("zookeeper registry created");
                    } else if (watchedEvent.getState() == Event.KeeperState.Expired) {
                        log.error("connect to zookeeper server timeout '{}'", sessionTimeOut);
                        reconnect();
                    } else if (watchedEvent.getState() == Event.KeeperState.Disconnected) {
                        log.info("disconnect to zookeeper server");
                        reconnect();
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

        //创建节点等一些预处理
        initRootZNode();
    }

    private void initRootZNode() {
        //如果没有创建根节点
        notExistAndCreate(RegistryConstants.REGISTRY_ROOT, null);
    }

    private void createZNode(String path, byte[] data) {
        //支持递归创建
        StringBuilder sb = new StringBuilder();
        String[] splits = path.split("/");
        for(int i = 1; i < splits.length; i++){
            sb.append("/" + splits[i]);
            try {
                this.zooKeeper.create(sb.toString(), data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            } catch (KeeperException e) {
                if(!(e instanceof KeeperException.NodeExistsException)){
                    log.error("", e);
                }
            } catch (InterruptedException e) {

            }
        }

        log.debug("create persistent znode(data= '{}') successfully>>> {}", data, path);

    }

    private void notExistAndCreate(String path, byte[] data) {
        try {
            if (this.zooKeeper.exists(path, false) == null) {
                log.debug("znode: '{}' not exist. now create", path);
                createZNode(path, data);
            }
        } catch (KeeperException e) {
            log.error("", e);
        } catch (InterruptedException e) {

        }
    }

    private void deleteZNode(String path) {
        try {
            this.zooKeeper.delete(path, -1);
            log.debug("delete znode successfully>>> " + path);
        } catch (KeeperException e) {
            log.error("", e);
        } catch (InterruptedException e) {

        }
    }

    private void deleteIfNoChilds(String path) {
        try {
            List<String> childs = this.zooKeeper.getChildren(path, false);
            if (childs == null || childs.size() <= 0) {
                deleteZNode(path);
            }
        } catch (KeeperException e) {
            log.error("", e);
        } catch (InterruptedException e) {

        }
    }

    @Override
    public void register(String serviceName, String host, int port){
        log.info("provider register service '{}' ", serviceName);
        String address = host + ":" + port;

        if (!HttpUtils.checkHostPort(address)) {
            throw new AddressFormatErrorException(address);
        }

        String serviceHostPath = RegistryConstants.getPath(serviceName, address);

        createZNode(serviceHostPath, null);
    }

    @Override
    public void unRegister(String serviceName, String host, int port) {
        log.info("provider unregister service '{}' ", serviceName);
        String address = host + ":" + port;

        String servicePath = RegistryConstants.getPath(serviceName);
        String serviceHostPath = RegistryConstants.getPath(serviceName, address);

        deleteZNode(serviceHostPath);
        deleteIfNoChilds(servicePath);
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
            zooKeeper.exists(RegistryConstants.getPath(directory.getServiceName()), (WatchedEvent watchedEvent) -> {
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
            });
        } catch (KeeperException e) {
            //服务还未注册或者因异常取消注册
            if (e.code().equals(KeeperException.Code.NONODE)) {
                //等待一段时间
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e1) {
                }
                //尝试重新订阅服务
                watch(directory);
            }
            else{
                log.error("", e);
            }
        } catch (InterruptedException e) {

        }
    }

    /**
     * 监听服务子节点
     */
    private void watchServiveNodeChilds(Directory directory){
        try {
            List<String> addresses = zooKeeper.getChildren(RegistryConstants.getPath(directory.getServiceName()),
                    (WatchedEvent watchedEvent) -> {
                        if (watchedEvent.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                            log.info("service '" + directory.getServiceName() + "' node childs changed");
                            watchServiveNodeChilds(directory);
                        }
                    });
            directory.discover(addresses);
        } catch (KeeperException e) {
            log.error("", e);
        } catch (InterruptedException e) {

        }
    }

    private void reconnect(){
        //断连时, 让所有directory持有的invoker失效
        for(Directory directory: DIRECTORY_CACHE.asMap().values()){
            directory.discover(Collections.emptyList());
        }
        //关闭原连接
        try {
            zooKeeper.close();
        } catch (InterruptedException e) {

        }
        zooKeeper = null;
        //重连
        connect();
    }

    @Override
    public void destroy() {
        if (zooKeeper != null) {
            try {
                log.info("zookeeper registry destroying...");
                zooKeeper.close();
                for(Directory directory: DIRECTORY_CACHE.asMap().values()){
                    directory.destroy();
                }
                DIRECTORY_CACHE.invalidateAll();
            } catch (InterruptedException e) {

            }
        }
        log.info("zookeeper registry destroy successfully");
    }

    //setter && getter
    public int getSessionTimeOut() {
        return sessionTimeOut;
    }

    public String getAddress() {
        return address;
    }
}
