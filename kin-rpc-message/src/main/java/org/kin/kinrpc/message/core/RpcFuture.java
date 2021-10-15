package org.kin.kinrpc.message.core;

import org.kin.framework.concurrent.OneLock;
import org.kin.kinrpc.transport.kinrpc.KinRpcAddress;

import java.io.Serializable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * request response消息返回的future
 *
 * @author huangjianqin
 * @date 2020-06-14
 */
public final class RpcFuture<R extends Serializable> implements Future<R> {
    /** rpc环境 */
    private final RpcEnv rpcEnv;
    /** rpc请求地址 */
    private final KinRpcAddress address;
    /** 请求唯一id */
    private final long requestId;
    /** 锁 */
    private final OneLock sync = new OneLock();
    /** 消息处理返回 */
    private volatile R reply;
    /** 消息处理异常 */
    private volatile Throwable exception;
    /** 标识future的取消状态 */
    private final AtomicBoolean cancelled = new AtomicBoolean();

    public RpcFuture(RpcEnv rpcEnv, KinRpcAddress address, long requestId) {
        this.rpcEnv = rpcEnv;
        this.address = address;
        this.requestId = requestId;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (!isDone() && cancelled.compareAndSet(false, true)) {
            TransportClient client = rpcEnv.getClient(address);
            client.removeInvalidWaitingResponseMessage(requestId);
        }
        return false;
    }

    @Override
    public boolean isCancelled() {
        return cancelled.get();
    }

    @Override
    public boolean isDone() {
        return sync.isDone() || isCancelled();
    }

    @Override
    public R get() {
        sync.acquire(-1);
        if (isDone()) {
            return reply;
        }
        return null;
    }

    @Override
    public R get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        boolean success = sync.tryAcquireNanos(-1, unit.toNanos(timeout));
        if (success) {
            if (isDone()) {
                return reply;
            } else {
                return null;
            }
        } else {
            cancel(true);
            throw new TimeoutException(getTimeoutMessage());
        }
    }

    private String getTimeoutMessage() {
        return "Timeout exception. Request id: " + requestId;
    }

    /**
     * 消息处理完并返回
     */
    void done(R reply) {
        if (isDone()) {
            return;
        }
        synchronized (this) {
            this.reply = reply;
            sync.release(1);
        }
    }

    /**
     * 消息处理完并返回, 但遇到错误
     */
    void fail(Throwable e) {
        if (isDone()) {
            return;
        }
        synchronized (this) {
            exception = e;
            sync.release(1);
        }
    }

    /**
     * @return 发送消息和消息处理的异常
     */
    Throwable getException() {
        return exception;
    }
}
