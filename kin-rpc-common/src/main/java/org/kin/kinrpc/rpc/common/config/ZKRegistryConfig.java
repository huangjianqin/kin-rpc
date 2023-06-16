package org.kin.kinrpc.rpc.common.config;

import org.kin.kinrpc.rpc.common.constants.Constants;

/**
 * Created by 健勤 on 2017/2/13.
 */
public class ZKRegistryConfig extends AbstractRegistryConfig {
    /** 会话超时 */
    private long sessionTimeout;

    ZKRegistryConfig(String adress) {
        super(adress);
        //连接注册中心的会话超时,以毫秒算,默认5s
        setSessionTimeout(Constants.SESSION_TIMEOUT);
    }

    //setter && getter
    public long getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    //--------------------------builder--------------------------

    /** 获取zookeeper注册中心配置builder */
    public static ZookeeperRegistryBuilder create(String address) {
        return new ZookeeperRegistryBuilder(address);
    }

    public static class ZookeeperRegistryBuilder {
        private ZKRegistryConfig registryConfig;

        private ZookeeperRegistryBuilder(String address) {
            this.registryConfig = new ZKRegistryConfig(address);
        }

        public ZookeeperRegistryBuilder sessionTimeout(long sessionTimeout) {
            registryConfig.sessionTimeout = sessionTimeout;
            return this;
        }

        public ZKRegistryConfig build() {
            return registryConfig;
        }
    }
}
