package org.kin.kinrpc;

/**
 * @author huangjianqin
 * @date 2023/6/19
 */
public interface Invoker<T> {
    /**
     * 服务调用
     *
     * @param invocation 服务调用相关元数据
     * @return服务调用结果
     */
    RpcResult invoke(Invocation invocation);
}
