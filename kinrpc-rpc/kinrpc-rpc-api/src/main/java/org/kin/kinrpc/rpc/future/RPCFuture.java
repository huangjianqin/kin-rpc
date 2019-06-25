package org.kin.kinrpc.rpc.future;


import org.kin.framework.concurrent.ThreadManager;
import org.kin.kinrpc.rpc.domain.RPCReference;
import org.kin.kinrpc.rpc.transport.domain.RPCRequest;
import org.kin.kinrpc.rpc.transport.domain.RPCResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * Created by 健勤 on 2017/2/15.
 */
public class RPCFuture implements Future<RPCResponse> {
    private static final Logger log = LoggerFactory.getLogger("transport");
    //所有RPCFuture实例共用一个线程池
    private static final ThreadManager THREADS = ThreadManager.forkJoinPoolThreadManager();

    //添加JVM关闭钩子,以确保释放该静态线程池
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            THREADS.shutdown();
        }));
    }

    //用于记录服务调用的耗时(毫秒),衡量负载
    private long startTime;
    private long responseTimeThreshold = 5000;

    private Sync sync;
    private RPCRequest request;
    private RPCResponse response;
    private List<AsyncRPCCallback> callbacks = new ArrayList<>();
    private RPCReference rpcReference;

    public RPCFuture(RPCRequest request, RPCReference rpcReference) {
        this.sync = new Sync();
        this.request = request;
        this.startTime = System.currentTimeMillis();
        this.rpcReference = rpcReference;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDone() {
        return sync.isDone();
    }

    @Override
    public RPCResponse get() throws InterruptedException, ExecutionException {
        sync.acquire(-1);
        if (isDone()) {
            return this.response;
        }
        return null;
    }

    @Override
    public RPCResponse get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        boolean success = sync.tryAcquireNanos(-1, unit.toNanos(timeout));
        if (success) {
            if (isDone()) {
                return this.response;
            } else {
                return null;
            }
        } else {
            throw new TimeoutException(getTimeoutMessage());
        }
    }

    private String getTimeoutMessage() {
        return "Timeout exception. Request id: " + this.request.getRequestId()
                + ". Request class name: " + this.request.getServiceName()
                + ". Request method: " + this.request.getMethod();
    }

    public void doneTimeout() {
        RPCResponse rpcResponse = RPCResponse.respWithError(request.getRequestId(),
                request.getServiceName(), request.getMethod(), getTimeoutMessage());
        done(rpcResponse);
    }

    public void done(RPCResponse response) {
        if (isDone()) {
            return;
        }
        this.response = response;
        rpcReference.removeInvalid(request);
        sync.release(1);
        THREADS.submit(() -> {
            for (AsyncRPCCallback callback : this.callbacks) {
                switch (response.getState()) {
                    case SUCCESS:
                        callback.success(response);
                        break;
                    case RETRY:
                        callback.retry(request);
                        break;
                    case ERROR:
                        callback.fail(new RuntimeException("Response error", new Throwable(response.getInfo())));
                        break;
                    default:
                        break;
                }
            }
        });

        long responseTime = System.currentTimeMillis() - startTime;
        if (responseTime > this.responseTimeThreshold) {
            log.info("service response time is too slow. Request id = '{}'. Response Time = {}ms", response.getRequestId(), responseTime);
        }
    }

    public RPCFuture addRPCCallback(AsyncRPCCallback callback) {
        if (!this.isDone()) {
            this.callbacks.add(callback);
        }

        return this;
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
