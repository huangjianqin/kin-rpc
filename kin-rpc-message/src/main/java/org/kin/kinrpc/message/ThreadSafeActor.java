package org.kin.kinrpc.message;

/**
 * 保证消息处理线程安全的{@link Actor}实现
 *
 * @author huangjianqin
 * @date 2020-06-16
 */
public abstract class ThreadSafeActor extends Actor {
    @Override
    public final boolean threadSafe() {
        return true;
    }
}
