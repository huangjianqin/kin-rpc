package org.kin.kinrpc.registry.directory;

import org.kin.kinrpc.ReferenceInvoker;

import java.util.List;

/**
 * 管理订阅服务的所有invoker实例
 *
 * @author huangjianqin
 * @date 2023/6/25
 */
public interface Directory {
    /**
     * 返回当前可用invokers
     *
     * @return 可用invokers
     */
    List<ReferenceInvoker<?>> list();

    /**
     * 返回directory是否available
     *
     * @return true表示directory available
     */
    boolean isAvailable();

    /**
     * 释放reference invoker占用资源
     */
    void destroy();

    /**
     * 返回服务唯一标识
     *
     * @return 服务唯一标识
     */
    String service();
}
