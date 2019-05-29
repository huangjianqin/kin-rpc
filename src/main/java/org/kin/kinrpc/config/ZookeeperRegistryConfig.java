package org.kin.kinrpc.config;

import org.apache.log4j.Logger;
import org.kin.kinrpc.common.Constants;
import org.kin.kinrpc.registry.zookeeper.ZookeeperRegistry;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.DataFormatException;

/**
 * Created by 健勤 on 2017/2/13.
 */
public class ZookeeperRegistryConfig {
    private static final Logger log = Logger.getLogger(ZookeeperRegistry.class);
    private static final AtomicInteger refCounter = new AtomicInteger(0);

    private final String host;
    private final int port;
    private String password;
    //连接注册中心的会话超时,以毫秒算,默认5s
    private int sessionTimeout = Constants.ZOOKEEPER_REGISTRY_DEFAULT_SESSIONTIMEOUT;

    private ZookeeperRegistry zookeeperRegistry;

    public ZookeeperRegistryConfig(String host) {
        this(host, Constants.ZOOKEEPER_REGISTRY_DEFAULT_PORT);
    }

    public ZookeeperRegistryConfig(String host, int port) {
        this.port = port;
        this.host = host;
    }

    /**
     * 检查配置参数正确性
     */
    private void checkConfig() {
        if (port < 0) {
            throw new IllegalStateException("zookeeper registry's port must be greater than 0");
        }

        if (sessionTimeout < 0) {
            throw new IllegalStateException("zookeeper registry's seesionTimeout must greater than 0");
        }

        if (host.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) {
            throw new IllegalStateException("zookeeper registry's host '" + host + "' format error");
        }
    }

    /**
     * config包下的类才可以调用此方法
     *
     * @return
     */
    ZookeeperRegistry getZookeeperRegistry() {
        checkConfig();

        log.info("getting zookeeper registry...");
        if (zookeeperRegistry == null) {
            synchronized (this) {
                if (zookeeperRegistry == null) {
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

    public void closeRegistry() {
        refCounter.getAndDecrement();
        if (refCounter.get() <= 0) {
            zookeeperRegistry.destroy();
        }
    }

    public String getAddress() {
        return this.host + ":" + this.port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }
}
