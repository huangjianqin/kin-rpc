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
     * 返回service invoker
     *
     * @return service invoker
     */
    ServiceInvoker<T> getInvoker();

    /**
     * unexport service
     */
    void unexport();
}
