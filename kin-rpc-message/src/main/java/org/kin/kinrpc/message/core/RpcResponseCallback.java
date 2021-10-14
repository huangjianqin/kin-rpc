package org.kin.kinrpc.message.core;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * 消息request response后, 响应返回回调接口
 *
 * @author huangjianqin
 * @date 2020-06-14
 */
public interface RpcResponseCallback {
    RpcResponseCallback EMPTY = new RpcResponseCallback() {
        @Override
        public <REQ extends Serializable, RESP extends Serializable> void onResponse(long requestId, REQ request, RESP response) {
            //do nothing
        }

        @Override
        public void onException(Throwable e) {
            //do nothing
        }
    };

    /**
     * 消息处理完并返回
     *
     * @param request  请求的消息
     * @param response 返回的消息
     */
    <REQ extends Serializable, RESP extends Serializable> void onResponse(long requestId, REQ request, RESP response);

    /**
     * 消息处理完并返回, 但遇到错误
     *
     * @param e 异常
     */
    void onException(Throwable e);

    /**
     * 执行callback操作的executor
     *
     * @return null则是使用 {@link RpcEnv#commonExecutors}
     */
    default ExecutorService executor() {
        return null;
    }

    /**
     * @return 执行callback操作的executor, 非null
     */
    static ExecutorService executor(RpcResponseCallback callback, RpcEnv rpcEnv) {
        ExecutorService executor = callback.executor();
        if (Objects.isNull(executor)) {
            executor = rpcEnv.commonExecutors;
        }
        return executor;
    }
}
