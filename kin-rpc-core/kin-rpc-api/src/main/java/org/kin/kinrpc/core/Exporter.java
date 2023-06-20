package org.kin.kinrpc.core;

/**
 * services exporter
 *
 * @author huangjianqin
 * @date 2020/11/3
 */
public interface Exporter<T> {
    /**
     * 获取provider invoker
     */
    Invoker<T> getInvoker();

    /**
     * unexport.
     * 相当于getInvoker().destroy();
     */
    void unexport();
}
