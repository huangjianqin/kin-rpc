package org.kin.kinrpc.config;

/**
 * Created by huangjianqin on 2019/6/18.
 */
public abstract class AbstractRegistryConfig extends AbstractConfig{
    protected String url;
    protected String password;
    //连接注册中心的会话超时,以毫秒算,默认5s
    protected int sessionTimeout = -1;

    AbstractRegistryConfig(String url) {
        this.url = url;
        this.password = "";
    }

    //setter && getter
    public String getUrl() {
        return url;
    }

    public String getPassword() {
        return password;
    }

    void setPassword(String password) {
        this.password = password;
    }

    public int getSessionTimeout() {
        return sessionTimeout;
    }

    void setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }
}
