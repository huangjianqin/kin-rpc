package org.kin.kinrpc;

/**
 * services exporter
 * todo
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
