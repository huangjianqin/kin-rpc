package org.kin.kinrpc.cluster;

import org.kin.framework.log.LoggerOprs;
import org.kin.framework.utils.ExtensionLoader;
import org.kin.kinrpc.rpc.RpcCallReturnAdapter;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * rpc call结果统一处理逻辑
 *
 * @author huangjianqin
 * @date 2021/2/6
 */
class RpcCallReturnAdapters implements RpcCallReturnAdapter, LoggerOprs {
    /** 单例 */
    static final RpcCallReturnAdapters INSTANCE = new RpcCallReturnAdapters();

    private RpcCallReturnAdapters() {
    }

    /**
     * rpc call结果统一处理逻辑
     *
     * @param returnType   服务接口返回类型
     * @param asyncRpcCall 是否是async rpc call
     * @param future       rpc async call返回的future
     * @return 合适的返回值
     */
    @Override
    public Object handleReturn(Class<?> returnType, boolean asyncRpcCall, CompletableFuture<?> future) throws Exception {
        if (asyncRpcCall) {
            //async rpc call
            RpcCallContext.updateFuture(future);
        }

        return getMatchedReturnAdapter(returnType).handleReturn(returnType, asyncRpcCall, future);
    }

    /**
     * 获取匹配的{@link RpcCallReturnAdapter}
     */
    private RpcCallReturnAdapter getMatchedReturnAdapter(Class<?> returnType) {
        for (RpcCallReturnAdapter returnAdapter : getResultAdapter()) {
            try {
                if (returnAdapter.match(returnType)) {
                    return returnAdapter;
                }
            } catch (Exception e) {
                error(String.format("'%s' adapter's method 'match' throw exception >>>", returnAdapter.getClass().getName()), e);
            }
        }

        return DefaultReturnAdapter.INSTANCE;
    }

    /**
     * 通过spi机制加载并返回已注册的{@link RpcCallReturnAdapter}
     */
    private List<RpcCallReturnAdapter> getResultAdapter() {
        return ExtensionLoader.getExtensions(RpcCallReturnAdapter.class);
    }

    @Override
    public boolean match(Class<?> returnType) {
        //工具类, 不参与类型匹配
        return false;
    }
}
