package org.kin.kinrpc.cluster.call;

import org.kin.framework.utils.SPI;

import java.util.concurrent.CompletableFuture;

/**
 * reference rpc call result adapter
 *
 * @author huangjianqin
 * @date 2021/2/6
 */
@SPI(alias = "rpcResultAdapter")
public interface RpcResultAdapter {
    /**
     * rpc call result处理
     *
     * @param type       服务方法返回类型
     * @param async      async rpc call or not
     * @param userFuture rpc call关联的future
     * @return rpc call result
     */
    Object convert(Class<?> type, boolean async, CompletableFuture<?> userFuture);

    /**
     * 标识{@code type}是否由该adapter处理
     *
     * @param type 服务方法方法返回类型
     * @return true表示服务方法方法返回结果由该adapter处理
     */
    boolean match(Class<?> type);
}
