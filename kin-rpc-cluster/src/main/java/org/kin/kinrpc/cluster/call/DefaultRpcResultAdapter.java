package org.kin.kinrpc.cluster.call;

import org.kin.framework.utils.ClassUtils;
import org.kin.framework.utils.ExceptionUtils;

import java.util.concurrent.CompletableFuture;

/**
 * 默认处理rpc call result逻辑
 *
 * @author huangjianqin
 * @date 2021/2/6
 */
class DefaultRpcResultAdapter implements RpcResultAdapter {
    static final DefaultRpcResultAdapter INSTANCE = new DefaultRpcResultAdapter();

    private DefaultRpcResultAdapter() {
    }

    @Override
    public Object convert(Class<?> type, boolean async, CompletableFuture<?> userFuture) {
        if (async) {
            //async rpc call
            //返回默认空值
            return ClassUtils.getDefaultValue(type);
        } else {
            //sync rpc call, sync 等待异步调用返回
            try {
                return userFuture.get();
            } catch (Exception e) {
                ExceptionUtils.throwExt(e);
                return null;
            }
        }
    }

    @Override
    public boolean match(Class<?> type) {
        //match any
        return true;
    }
}
