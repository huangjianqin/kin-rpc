package org.kin.kinrpc.registry;

import org.kin.kinrpc.common.URL;

/**
 * Created by huangjianqin on 2019/6/18.
 */
public interface RegistryFactory {
    /**
     * 根据url获取注册中心
     *
     * @param url 注册中心地址
     * @return 注册中心实例
     */
    Registry getRegistry(URL url);

    /**
     * 销毁指定注册中心
     * @param url 注册中心实例
     */
    void close(URL url);
}
