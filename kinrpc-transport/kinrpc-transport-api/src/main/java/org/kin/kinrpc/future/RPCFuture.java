package org.kin.kinrpc.future;


import org.kin.framework.concurrent.ThreadManager;
import org.kin.kinrpc.transport.rpc.domain.RPCRequest;
import org.kin.kinrpc.transport.rpc.domain.RPCResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by 健勤 on 2017/2/15.
 */
public class RPCFuture implements Future<RPCResponse> {
    private static final Logger log = LoggerFactory.getLogger("transport");

    private Sync sync;
    private RPCRequest request;
    private RPCResponse response;

    //所有RPCFuture实例共用一个线程池
    private static final ThreadManager threads = ThreadManager.forkJoinPoolThreadManager();

    //添加JVM关闭钩子,以确保释放该静态线程池
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            threads.shutdown();
        }));
    }

    private List<AsyncRPCCallback> callbacks = new ArrayList<>();
    private ReentrantLock lock = new ReentrantLock();

    //用于记录服务调用的耗时(毫秒),衡量负载
    private long startTime;
    private long responseTimeThreshold = 5000;

    public RPCFuture(RPCRequest request) {
        this.sync = new Sync();
        this.request = request;
        this.startTime = System.currentTimeMillis();
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    public boolean isCancelled() {
        throw new UnsupportedOperationException();
    }

    public boolean isDone() {
        return sync.isDone();
    }

    public RPCResponse get() throws InterruptedException, ExecutionException {
        sync.acquire(-1);
        if (isDone()) {
            return this.response;
        }
        return null;
    }

    public RPCResponse get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        boolean success = sync.tryAcquireNanos(-1, unit.toNanos(timeout));
        if (success) {
            if (isDone()) {
                return this.response;
            } else {
                return null;
            }
        } else {
            throw new RuntimeException("Timeout exception. Request id: " + this.request.getRequestId()
                    + ". Request class name: " + this.request.getServiceName()
                    + ". Request method: " + this.request.getMethod());
        }
    }

    public void done(RPCResponse response) {
        if (isDone()) {
            return;
        }
        sync.release(1);
        this.response = response;
        invokeAllCallBacks();

        long responseTime = System.currentTimeMillis() - startTime;
        if (responseTime > this.responseTimeThreshold) {
            log.info("Service response time is too slow. Request id = '{}'. Response Time = {}ms", response.getRequestId(), responseTime);
        }
    }

    public RPCFuture addRPCCallback(AsyncRPCCallback callback) {
        lock.lock();
        try {
            if (this.isDone()) {
                runCallBack(callback);
            } else {
                this.callbacks.add(callback);
            }
        } finally {
            lock.unlock();
        }

        return this;
    }

    private void runCallBack(final AsyncRPCCallback callback) {
        RPCResponse response = this.response;
        threads.submit(() -> {
            if (!response.getState().equals(RPCResponse.State.ERROR)) {
                callback.success(response);
            } else {
                callback.fail(new RuntimeException("Response error", new Throwable(response.getInfo())));
            }
        });
    }

    private void invokeAllCallBacks() {
        lock.lock();

        try {
            for (AsyncRPCCallback callback : this.callbacks) {
                runCallBack(callback);
            }
        } finally {
            lock.unlock();
        }
    }

    class Sync extends AbstractQueuedSynchronizer {
        private final int DONE = 1;
        private final int PENDING = 0;

        @Override
        protected boolean tryAcquire(int acquires) {
            return getState() == DONE ? true : false;
        }

        @Override
        protected boolean tryRelease(int releases) {
            if (getState() == PENDING) {
                if (compareAndSetState(PENDING, DONE)) {
                    return true;
                }
            }

            return false;
        }

        public boolean isDone() {
            return getState() == DONE;
        }
    }

    public RPCRequest getRequest() {
        return request;
    }
}