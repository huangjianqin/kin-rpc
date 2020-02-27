package org.kin.kinrpc.registry;


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
     * @param serviceName 服务名
     * @param host        hostname
     * @param port        端口
     */
    void register(String serviceName, String host, int port);

    /**
     * 注销服务
     * @param serviceName 服务名
     * @param host hostname
     * @param port 端口
     */
    void unRegister(String serviceName, String host, int port);

    /**
     * 订阅服务
     * @param serviceName 服务名
     * @param connectTimeout 连接超时
     * @return 服务订阅目录, 包含所有可用invokers
     */
    Directory subscribe(String serviceName, int connectTimeout);

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
