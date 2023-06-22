package org.kin.kinrpc.transport;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

/**
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
}
