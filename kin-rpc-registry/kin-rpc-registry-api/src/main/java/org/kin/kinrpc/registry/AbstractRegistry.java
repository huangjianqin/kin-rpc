package org.kin.kinrpc.registry;

import org.kin.kinrpc.config.RegistryConfig;

/**
 * todo regsitry是否需要缓存directory
 * Created by huangjianqin on 2019/6/25.
 */
public abstract class AbstractRegistry implements Registry {
    /** 注册中心配置 */
    protected final RegistryConfig config;

    protected AbstractRegistry(RegistryConfig config) {
        this.config = config;
    }

    //getter
    public RegistryConfig getConfig() {
        return config;
    }
}
