package org.kin.kinrpc.config;

/**
 * Created by huangjianqin on 2019/6/18.
 */
public abstract class AbstractRegistryConfig extends AbstractConfig {
    /** 注册中心地址 */
    protected String address;
    /** 会话超时 */
    protected long sessionTimeout;
    /** 观察服务变化间隔, 目前仅用于redis */
    protected long watchInterval;

    AbstractRegistryConfig(String address) {
        this.address = address;
    }

    //setter && getter

    public String getAddress() {
        return address;
    }

    public long getSessionTimeout() {
        return sessionTimeout;
    }

    void setSessionTimeout(long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public long getWatchInterval() {
        return watchInterval;
    }

    public void setWatchInterval(long watchInterval) {
        this.watchInterval = watchInterval;
    }
}
