package org.kin.kinrpc.registry;


import org.kin.kinrpc.rpc.common.Url;

/**
 * Created by 健勤 on 2016/10/9.
 */

public interface Registry {
    /**
     * 连接注册中心
     */
    void connect();

    /**
     * 注册服务
     *
     * @param url 服务配置
     */
    void register(Url url);

    /**
     * 注销服务
     *
     * @param url 服务配置
     */
    void unRegister(Url url);

    /**
     * 订阅服务
     *
     * @param serviceName 服务名
     * @return 服务订阅目录, 包含所有可用invokers
     */
    Directory subscribe(String serviceName);

    /**
     * 取消订阅服务
     * @param serviceName 服务名
     */
    void unSubscribe(String serviceName);

    /**
     * retain
     * 增加引用数
     * 在同一jvm下, 该对象是否还有引用
     */
    void retain();

    /**
     * 释放引用数
     * @return 是否释放引用数成功(引用数不够)
     */
    boolean release();

    /**
     * 销毁注册中心
     */
    void destroy();
}
