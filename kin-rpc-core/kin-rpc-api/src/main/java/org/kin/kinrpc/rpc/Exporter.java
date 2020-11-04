package org.kin.kinrpc.rpc;

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
     * todo 相当于getInvoker().destroy();
     */
    void unexport();
}
