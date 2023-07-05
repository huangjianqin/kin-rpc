package org.kin.kinrpc;

/**
 * reference端{@link Invoker}实现
 * <p>
 * !!! 注意要实现{@link Object#equals(Object)}和{@link Object#hashCode()}
 *
 * @author huangjianqin
 * @date 2023/6/24
 */
public interface ReferenceInvoker<T> extends Invoker<T> {
    /**
     * invoker引用的service信息
     *
     * @return invoker引用的service信息
     */
    ServiceInstance serviceInstance();

    /**
     * 返回invoker available or not
     *
     * @return true表示invoker available
     */
    boolean isAvailable();

    /**
     * 关闭底层client, 释放资源
     */
    void destroy();
}
