package org.kin.kinrpc.config;

/**
 * Created by huangjianqin on 2019/6/18.
 */
public abstract class AbstractRegistryConfig extends AbstractConfig{
    protected String address;
    //连接注册中心的会话超时,以毫秒算,默认5s
    protected int sessionTimeout = -1;

    AbstractRegistryConfig(String address) {
        this.address = address;
    }

    //setter && getter
    public String getAddress() {
        return address;
    }

    public int getSessionTimeout() {
        return sessionTimeout;
    }

    void setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }
}
