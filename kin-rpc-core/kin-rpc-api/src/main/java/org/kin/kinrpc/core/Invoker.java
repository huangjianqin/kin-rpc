package org.kin.kinrpc.core;

import org.kin.kinrpc.config.AbstractInterfaceConfig;

/**
 * Created by 健勤 on 2017/2/11.
 */
public interface Invoker<T> {
    /**
     * 服务调用
     *
     * @param invocation 服务调用相关元数据
     * @return服务调用结果
     */
    RpcResult invoke(Invocation invocation);

    /**
     * 返回服务接口
     *
     * @return 服务接口
     */
    Class<T> getInterface();

    /**
     * 返回invoker配置
     *
     * @return invoker配置, {@link org.kin.kinrpc.config.ServiceConfig}实例或者{@link org.kin.kinrpc.config.ReferenceConfig}实例
     */
    AbstractInterfaceConfig<T, ?> config();

    /**
     * 清理资源占用
     */
    void destroy();
}
