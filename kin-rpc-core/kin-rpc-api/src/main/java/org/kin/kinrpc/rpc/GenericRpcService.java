package org.kin.kinrpc.rpc;

/**
 * 不同协议间抽象的rpc远程调用服务
 *
 * @author huangjianqin
 * @date 2020/11/16
 */
@FunctionalInterface
public interface GenericRpcService {
    /**
     * 调用抽象
     */
    Object invoke(String method, Object[] args) throws Exception;
}
