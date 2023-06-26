package org.kin.kinrpc.cluster.call;

import org.kin.framework.utils.ExtensionLoader;

import java.util.concurrent.CompletableFuture;

/**
 * @author huangjianqin
 * @date 2023/6/26
 */
public final class RpcResultAdapterHelper {
    private RpcResultAdapterHelper() {
    }

    /**
     * rpc call result处理
     *
     * @param type       服务方法返回类型
     * @param async      async rpc call or not
     * @param userFuture rpc call关联的future
     * @return rpc call result
     */
    public static Object convert(Class<?> type, boolean async, CompletableFuture<?> userFuture) {
        return getMatchedAdapter(type).convert(type, async, userFuture);
    }

    /**
     * 返回服务方法返回值类型匹配的{@link  RpcResultAdapter}实例
     */
    private static RpcResultAdapter getMatchedAdapter(Class<?> type) {
        for (RpcResultAdapter adapter : ExtensionLoader.getExtensions(RpcResultAdapter.class)) {
            if (adapter.match(type)) {
                return adapter;
            }
        }

        return DefaultRpcResultAdapter.INSTANCE;
    }
}
