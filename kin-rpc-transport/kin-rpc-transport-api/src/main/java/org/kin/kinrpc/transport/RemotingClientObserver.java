package org.kin.kinrpc.transport;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

/**
 * todo 命名优化
 *
 * @author huangjianqin
 * @date 2023/6/21
 */
public interface RemotingClientObserver {
    /**
     * 发送心跳
     *
     * @return 心跳response
     */
    CompletableFuture<Void> heartbeat();

    /**
     * remoting client -> unhealth
     */
    void toUnhealth();

    /**
     * remoting client -> health
     */
    void toHealth();

    /**
     * 发起重连
     *
     * @return reconnect signal
     */
    @Nullable
    CompletableFuture<Void> reconnect();

    /**
     * 返回client name
     *
     * @return client name
     */
    String getName();

    /**
     * 返回remoting client是否已经terminated
     */
    boolean isTerminated();
}
