package org.kin.kinrpc.registry;


import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.registry.directory.Directory;

/**
 * 注册中心
 * Created by 健勤 on 2016/10/9.
 */
public interface Registry {
    /**
     * 初始化
     */
    void init();

    /**
     * 注册服务
     *
     * @param serviceConfig 服务配置
     */
    void register(ServiceConfig<?> serviceConfig);

    /**
     * 注销服务
     *
     * @param serviceConfig 服务配置
     */
    void unregister(ServiceConfig<?> serviceConfig);

    /**
     * 服务订阅
     *
     * @param config reference config
     * @return {@link Directory}实例
     */
    Directory subscribe(ReferenceConfig<?> config);

    /**
     * 取消服务订阅
     *
     * @param service 服务gsv
     */
    void unsubscribe(String service);

    /**
     * 释放注册中心占用资源
     */
    void destroy();
}
