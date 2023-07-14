package org.kin.kinrpc.message;

import java.util.concurrent.ExecutorService;

/**
 * message response后回调接口
 *
 * @author huangjianqin
 * @date 2020-06-14
 */
public interface MessageCallback {
    /**
     * message response success callback
     *
     * @param request  request message
     * @param response response message
     */
    <REQ, RESP> void onSuccess(REQ request, RESP response);

    /**
     * message response fail callback
     *
     * @param e 异常
     */
    void onFailure(Throwable e);

    /**
     * 执行回调的executor
     *
     * @return null则是使用 {@link ActorEnv#commonExecutors}
     */
    default ExecutorService executor() {
        return null;
    }
}
