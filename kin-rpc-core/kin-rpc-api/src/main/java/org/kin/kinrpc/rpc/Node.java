package org.kin.kinrpc.rpc;

import org.kin.kinrpc.rpc.common.Url;

/**
 * invoker 节点
 *
 * @author huangjianqin
 * @date 2020/11/4
 */
public interface Node {
    /**
     * @return invoker 对应的url
     */
    Url url();

    /**
     * @return 是否可用
     */
    boolean isAvailable();

    /**
     * 清理资源占用
     */
    void destroy();
}
