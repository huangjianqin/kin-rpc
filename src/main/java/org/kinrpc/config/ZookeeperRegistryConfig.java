package org.kinrpc.config;

import org.apache.log4j.Logger;
import org.kinrpc.registry.zookeeper.ZookeeperRegistry;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.DataFormatException;

/**
 * Created by 健勤 on 2017/2/13.
 */
public class ZookeeperRegistryConfig {
    private static final Logger log = Logger.getLogger(ZookeeperRegistry.class);
    private static final AtomicInteger refCounter = new AtomicInteger(0);

    private String host;
    private int port;
    private String password;
    //连接注册中心的会话超时,以毫秒算,默认5s
    private int sessionTimeOut = 5000;

    private ZookeeperRegistry zookeeperRegistry;

    public ZookeeperRegistry getZookeeperRegistry() {
        log.info("getting zookeeper registry...");
        if(zookeeperRegistry == null){
            synchronized (this){
                if(zookeeperRegistry == null){
                    zookeeperRegistry = new ZookeeperRegistry(this);
                    try {
                        zookeeperRegistry.connect();
                    } catch (DataFormatException e) {
                        log.error("zookeeper registry address format error");
                        e.printStackTrace();
                    }
                }
            }
        }

        return zookeeperRegistry;
    }

    public void closeRegistry(){
        refCounter.getAndDecrement();
        if(refCounter.get() <= 0){
            zookeeperRegistry.destroy();
        }
    }

    public String getAddress(){
        String address = host;
        if(port > 0 && port < Integer.MAX_VALUE){
            address += (":" + port);
        }
        else{
            //采用默认
            address += ":2181";
        }

        return address;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getSessionTimeOut() {
        return sessionTimeOut;
    }

    public void setSessionTimeOut(int sessionTimeOut) {
        this.sessionTimeOut = sessionTimeOut;
    }
}
