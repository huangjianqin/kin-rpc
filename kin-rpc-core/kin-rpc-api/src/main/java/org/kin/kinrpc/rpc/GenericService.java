package org.kin.kinrpc.rpc;

/**
 * 泛化服务, 仅提供方法名和方法参数即可完成一次rpc call
 *
 * @author huangjianqin
 * @date 2020/11/16
 */
@FunctionalInterface
public interface GenericService {
    /**
     * 调用抽象
     */
    Object invoke(String method, Object[] args) throws Throwable;
}
