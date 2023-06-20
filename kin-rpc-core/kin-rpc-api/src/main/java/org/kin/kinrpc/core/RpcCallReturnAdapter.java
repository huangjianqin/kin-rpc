package org.kin.kinrpc.core;

import org.kin.framework.utils.ClassUtils;
import org.kin.framework.utils.SPI;

import java.util.concurrent.CompletableFuture;

/**
 * reference rpc call返回值适配器
 *
 * @author huangjianqin
 * @date 2021/2/6
 */
@SPI(alias = "resultAdapter")
public interface RpcCallReturnAdapter {
    /**
     * @param returnType   服务接口返回类型
     * @param asyncRpcCall 是否是async rpc call
     * @param future       rpc async call返回的future
     * @return 合适的返回值
     */
    default Object handleReturn(Class<?> returnType, boolean asyncRpcCall, CompletableFuture<?> future) throws Exception {
        if (asyncRpcCall) {
            //返回默认空值
            return ClassUtils.getDefaultValue(returnType);
        } else {
            //sync rpc call, sync 等待异步调用返回
            try {
                return future.get();
            } catch (Exception e) {
                throw e;
            }
        }
    }

    /**
     * @return 是否可以处理指定返回类型
     */
    boolean match(Class<?> returnType);
}
