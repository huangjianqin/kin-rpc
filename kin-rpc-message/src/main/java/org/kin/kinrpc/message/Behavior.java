package org.kin.kinrpc.message;

/**
 * 定义消息处理逻辑
 *
 * @author huangjianqin
 * @date 2023/7/13
 */
@FunctionalInterface
public interface Behavior<M> {
    /**
     * 定义消息处理逻辑
     *
     * @param message received message
     */
    void onReceive(M message);
}
