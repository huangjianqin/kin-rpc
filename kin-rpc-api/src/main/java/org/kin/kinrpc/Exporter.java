package org.kin.kinrpc;

/**
 * service exporter
 *
 * @author huangjianqin
 * @date 2020/11/3
 */
public interface Exporter<T> {
    /**
     * 返回service
     *
     * @return service
     */
    RpcService<T> service();

    /**
     * unexport service
     */
    void unexport();
}
