package org.kin.kinrpc.bootstrap;

import org.kin.framework.utils.SPI;
import org.kin.kinrpc.config.ReferenceConfig;

/**
 * @author huangjianqin
 * @date 2023/6/20
 */
@SPI(singleton = false)
public abstract class ReferenceBootstrap<T> {
    /** 服务引用配置 */
    protected final ReferenceConfig<T> config;

    protected ReferenceBootstrap(ReferenceConfig<T> config) {
        this.config = config;
    }

    /**
     * 创建服务引用代理实例
     *
     * @return 服务引用代理实例
     */
    public abstract T refer();

    /**
     * 取消服务引用
     */
    public abstract void unRefer();

    //getter
    public ReferenceConfig<T> getConfig() {
        return config;
    }
}
