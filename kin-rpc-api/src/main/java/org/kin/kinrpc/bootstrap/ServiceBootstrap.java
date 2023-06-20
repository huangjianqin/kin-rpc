package org.kin.kinrpc.bootstrap;

import org.kin.framework.utils.SPI;
import org.kin.kinrpc.config.ServiceConfig;

/**
 * @author huangjianqin
 * @date 2023/6/20
 */
@SPI(singleton = false)
public abstract class ServiceBootstrap<T> {
    /** 服务配置 */
    protected final ServiceConfig<T> config;

    protected ServiceBootstrap(ServiceConfig<T> config) {
        this.config = config;
    }

    /**
     * 暴露服务
     */
    public abstract void export();

    /**
     * 服务下线
     */
    public abstract void unExport();

    //getter
    public ServiceConfig<T> getConfig() {
        return config;
    }
}
