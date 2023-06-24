package org.kin.kinrpc;

/**
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
}
