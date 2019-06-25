package org.kin.kinrpc.config;

/**
 * Created by huangjianqin on 2019/6/18.
 */
abstract class RegistryConfig extends Config{
    protected String url;
    protected String password;
    //连接注册中心的会话超时,以毫秒算,默认5s
    protected int sessionTimeout = -1;

    public RegistryConfig(String url) {
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
