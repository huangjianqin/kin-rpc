package org.kin.kinrpc.springboot;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 配置详情, 请看{@link org.kin.kinrpc.config.ZookeeperRegistryConfig}
 *
 * @author huangjianqin
 * @date 2020/12/13
 */
@ConfigurationProperties("kin.rpc.zookeeper")
public class ZookeeperRegistryProperties {
    /** zk集群地址 */
    private String address;
    /** zk client session time out */
    private long sessionTimeout;

    //setter && getter

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public long getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }
}
