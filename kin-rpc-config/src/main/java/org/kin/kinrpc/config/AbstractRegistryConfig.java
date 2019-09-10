package org.kin.kinrpc.config;

/**
 * Created by huangjianqin on 2019/6/18.
 */
public abstract class AbstractRegistryConfig extends AbstractConfig {
    protected String address;
    protected int sessionTimeout;

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
