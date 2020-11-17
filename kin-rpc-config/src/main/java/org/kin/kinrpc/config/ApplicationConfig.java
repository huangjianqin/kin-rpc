package org.kin.kinrpc.config;

import com.google.common.base.Preconditions;
import org.kin.framework.utils.StringUtils;

/**
 * Created by 健勤 on 2017/2/12.
 */
public class ApplicationConfig extends AbstractConfig {
    /** 应用名 */
    private String appName;

    ApplicationConfig() {
        //获取最外层调用的class name
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        String className = elements[elements.length - 1].getClassName();
        int last = className.lastIndexOf(".");
        //默认是main方法的simple class name
        appName = className.substring(last + 1);
    }

    ApplicationConfig(String appName) {
        this.appName = appName;
    }

    @Override
    void check() {
        Preconditions.checkArgument(StringUtils.isNotBlank(appName));
    }

    //setter && getter

    public String getAppName() {
        return appName;
    }
}
