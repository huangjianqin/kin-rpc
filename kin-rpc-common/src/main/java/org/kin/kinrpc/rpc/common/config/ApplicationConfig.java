package org.kin.kinrpc.rpc.common.config;

import com.google.common.base.Preconditions;
import org.kin.framework.utils.StringUtils;

/**
 * Created by 健勤 on 2017/2/12.
 */
public class ApplicationConfig extends AbstractConfig {
    /** 应用名 */
    private String app;

    ApplicationConfig() {
        //获取最外层调用的class name
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        String className = elements[elements.length - 1].getClassName();
        int last = className.lastIndexOf(".");
        //默认是main方法的simple class name
        app = className.substring(last + 1);
    }

    ApplicationConfig(String app) {
        this.app = app;
    }

    @Override
    void check() {
        Preconditions.checkArgument(StringUtils.isNotBlank(app));
    }

    //setter && getter

    public String getApp() {
        return app;
    }
}
