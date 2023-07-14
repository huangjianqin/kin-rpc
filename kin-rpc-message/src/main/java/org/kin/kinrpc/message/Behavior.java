package org.kin.kinrpc.message;

import java.io.Serializable;

/**
 * 定义消息处理逻辑
 *
 * @author huangjianqin
 * @date 2023/7/13
 */
@FunctionalInterface
public interface Behavior<M extends Serializable> {
    /**
     * 定义消息处理逻辑
     *
     * @param actorContext actor context
     * @param message      received message
     */
    void onReceive(ActorContext actorContext, M message);
}
