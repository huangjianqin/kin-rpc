package org.kin.kinrpc.transport;

/**
 * @author huangjianqin
 * @date 2023/7/12
 */
public interface RemotingClientStateObserver {
    /**
     * 定义连接成功后额外逻辑
     */
    default void onConnectSuccess(RemotingClient client) {
        //default do nothing
    }

    /**
     * 定义重连成功后额外逻辑
     */
    default void onReconnectSuccess(RemotingClient client) {
        //default do nothing
    }

    /**
     * 定义client terminated后额外逻辑
     */
    default void onTerminated(RemotingClient client) {
        //default do nothing
    }
}
