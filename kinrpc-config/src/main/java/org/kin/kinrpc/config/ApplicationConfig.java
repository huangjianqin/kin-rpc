package org.kin.kinrpc.config;

import com.google.common.base.Preconditions;
import org.kin.framework.utils.StringUtils;

/**
 * Created by 健勤 on 2017/2/12.
 */
class ApplicationConfig extends Config{
    private String appName;

    public ApplicationConfig() {
        //获取最外层调用的class name
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        appName = elements[elements.length - 1].getClassName();
    }

    public ApplicationConfig(String appName) {
        this.appName = appName;
    }

    @Override
    public void check() {
        Preconditions.checkArgument(StringUtils.isNotBlank(appName));
    }

    //setter && getter
    public String getAppName() {
        return appName;
    }
}
