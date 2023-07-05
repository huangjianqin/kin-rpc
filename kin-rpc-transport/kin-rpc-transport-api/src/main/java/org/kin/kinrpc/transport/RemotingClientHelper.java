package org.kin.kinrpc.transport;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

/**
 * 目的是{@link RemotingClient}不对外暴露这些接口, 这些接口仅仅提供给{@link RemotingClientHealthManager}使用
 *
 * @author huangjianqin
 * @date 2023/6/21
 */
public interface RemotingClientHelper {
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
