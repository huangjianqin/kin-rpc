package org.kin.kinrpc.config;

import org.kin.framework.utils.StringUtils;

/**
 * 应用配置
 *
 * @author huangjianqin
 * @date 2023/6/16
 */
public class ApplicationConfig extends AbstractConfig {
    /** 应用名 */
    private String appName;

    public static ApplicationConfig create(String appName) {
        return new ApplicationConfig().appName(appName);
    }

    private ApplicationConfig() {
    }

    @Override
    public void checkValid() {
        super.checkValid();
        check(StringUtils.isNotBlank(appName), "appName must be not blank");
    }

    //setter && getter
    public String getAppName() {
        return appName;
    }

    public ApplicationConfig appName(String appName) {
        this.appName = appName;
        return this;
    }

    @Override
    public String toString() {
        return "ApplicationConfig{" +
                "appName='" + appName + '\'' +
                '}';
    }
}
