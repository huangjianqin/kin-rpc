package org.kin.kinrpc.registry.zookeeper;

import org.apache.log4j.Logger;
import org.apache.zookeeper.*;
import org.kin.kinrpc.common.Constants;
import org.kin.kinrpc.config.ZookeeperRegistryConfig;
import org.kin.kinrpc.registry.AbstractRegistry;
import org.kin.kinrpc.rpc.cluster.ZookeeperDirectory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.zip.DataFormatException;

/**
 * Created by 健勤 on 2016/10/10.
 */
public class ZookeeperRegistry extends AbstractRegistry {
    private static final Logger log = Logger.getLogger(ZookeeperRegistry.class);
    private CountDownLatch countDownLatch = new CountDownLatch(1);

    private ZooKeeper zooKeeper;
    private int sessionTimeOut;

    public ZookeeperRegistry(ZookeeperRegistryConfig registryConfig) {
        super(registryConfig.getAddress(), registryConfig.getPassword());
        this.sessionTimeOut = registryConfig.getSessionTimeout();
    }

    public void connect() throws DataFormatException {
        log.info("zookeeper registry creating...");

        if(!address.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d{1,5}")){
            throw new DataFormatException("zookeeper registry address('" + address + "') format error");
        }
        String host = address.split(":")[0];

        try {
            this.zooKeeper = new ZooKeeper(host, this.sessionTimeOut, new Watcher() {
                public void process(WatchedEvent watchedEvent) {
                    if(watchedEvent.getState() == Event.KeeperState.SyncConnected){
                        countDownLatch.countDown();
                    }
                    else if(watchedEvent.getState() == Event.KeeperState.Expired){
                        log.error("connect to zookeeper server timeout(" + sessionTimeOut + ")");
                        throw new RuntimeException("connect to zookeeper server timeout(" + sessionTimeOut + ")");
                    }
                    else if(watchedEvent.getState() == Event.KeeperState.Disconnected){
                        log.info("disconnect to zookeeper server");
                        throw new RuntimeException("disconnect to zookeeper server");
                    }
                }
            });
        } catch (IOException e) {
            log.error("zookeeper client connect error");
            e.printStackTrace();
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //一些预处理
        initRootZNode();
    }

    private void initRootZNode() {
        //如果没有创建根节点
        notExistAndCreate(Constants.REGISTRY_ROOT, null);
    }

    private void createZNode(String path, byte[] data){
        try {
            this.zooKeeper.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            log.info("create znode successfully>>> " + path);
            log.info("this znode's data>>> " + data);
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void deleteZNode(String path) {
        try {
            this.zooKeeper.delete(path, -1);
            log.info("delete znode successfully>>> " + path);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }
    }

    private void notExistAndCreate(String path, byte[] data){
        try {
            if(this.zooKeeper.exists(path, false) == null){
                log.info("znode: " + path + " >>> not exist. now create");
                createZNode(path, data);
            }
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void noChildsAndDelete(String path){
        try {
            List<String> childs = this.zooKeeper.getChildren(path, false);
            if(childs == null || childs.size() <= 0){
                deleteZNode(path);
            }
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void register(String serviceName, String host, int port) throws DataFormatException {
        log.info("provider register service '" + serviceName + "' " + ">>> zookeeper registry(" + getAddress() + ")");
        String address = host + ":" + port;

        if(!address.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d{1,5}")){
            throw new DataFormatException("zookeeper registry address('" + address + "') format error");
        }

        String servicePath = Constants.REGISTRY_ROOT + Constants.REGISTRY_PAHT_SEPARATOR + serviceName;
        String serviceHostPath = Constants.REGISTRY_ROOT + Constants.REGISTRY_PAHT_SEPARATOR + serviceName + Constants.REGISTRY_PAHT_SEPARATOR + address;

        notExistAndCreate(servicePath, null);
        createZNode(serviceHostPath, null);
    }

    public void unRegister(String serviceName, String host, int port) {
        log.info("provider unregister service '" + serviceName + "' " + ">>> zookeeper registry(" + getAddress() + ")");
        String address = host + ":" + port;

        String servicePath = Constants.REGISTRY_ROOT + Constants.REGISTRY_PAHT_SEPARATOR + serviceName;
        String serviceHostPath = Constants.REGISTRY_ROOT + Constants.REGISTRY_PAHT_SEPARATOR + serviceName + Constants.REGISTRY_PAHT_SEPARATOR + address;

        deleteZNode(serviceHostPath);
        noChildsAndDelete(servicePath);
    }


    public ZookeeperDirectory subscribe(Class<?> interfaceClass, int connectTimeout) {
        log.info("consumer subscribe service '" + interfaceClass.getName() + "' " + ">>> zookeeper registry(" + getAddress() + ")");
        return new ZookeeperDirectory(this, interfaceClass, connectTimeout);
    }

    public void destroy() {
        if(zooKeeper != null){
            try {
                log.info("zookeeper registry destroying...");
                zooKeeper.close();
            } catch (InterruptedException e) {
                e.printStackTrace();
                //恢复中断标识
//                Thread.currentThread().interrupt();
            }
        }
        log.info("zookeeper registry destroy successfully");
    }

    public int getSessionTimeOut() {
        return sessionTimeOut;
    }

    public ZooKeeper getConnection(){
        return this.zooKeeper;
    }
}
