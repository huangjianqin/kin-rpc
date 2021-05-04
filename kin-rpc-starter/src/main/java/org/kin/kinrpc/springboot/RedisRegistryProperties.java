package org.kin.kinrpc.springboot;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 配置详情, 请看{@link org.kin.kinrpc.config.RedisRegistryConfig}
 *
 * @author huangjianqin
 * @date 2020/12/13
 */
@ConfigurationProperties("kin.rpc.redis")
public class RedisRegistryProperties {
    private String address;
    private long watchInterval;

    //setter && getter
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public long getWatchInterval() {
        return watchInterval;
    }

    public void setWatchInterval(long watchInterval) {
        this.watchInterval = watchInterval;
    }
}
