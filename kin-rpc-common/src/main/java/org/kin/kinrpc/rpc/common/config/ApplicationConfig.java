package org.kin.kinrpc.rpc.common.config;

/**
 * 应用配置
 *
 * @author huangjianqin
 * @date 2023/6/16
 */
public class ApplicationConfig implements Config {
    /** 应用名 */
    private String appName;

    public static ApplicationConfig create(String appName) {
        return new ApplicationConfig().appName(appName);
    }

    private ApplicationConfig() {
    }

    //setter && getter
    public String getAppName() {
        return appName;
    }

    public ApplicationConfig appName(String appName) {
        this.appName = appName;
        return this;
    }
}
