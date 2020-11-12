package org.kin.kinrpc.rpc;


import org.kin.framework.concurrent.lock.OneLock;
import org.kin.kinrpc.rpc.exception.RpcCallCancelledException;
import org.kin.kinrpc.rpc.exception.RpcCallErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by 健勤 on 2017/2/15.
 */
public class RpcFuture implements Future<RpcResponse> {
    private static final Logger log = LoggerFactory.getLogger(RpcFuture.class);

    /** 用于记录服务调用的耗时(毫秒),衡量负载 */
    private long startTime;
    private long responseTimeThreshold = 5000;

    private OneLock sync;
    private RpcRequest request;
    private RpcResponse response;
    private List<AsyncRpcCallback> callbacks = new ArrayList<>();
    private AtomicBoolean cancelled = new AtomicBoolean();

    public RpcFuture(RpcRequest request) {
        this.sync = new OneLock();
        this.request = request;
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (!isDone() && cancelled.compareAndSet(false, true)) {
            doneError(new RpcCallCancelledException("rpc call canncelled >>>".concat(request.toString())));
            return true;
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
    public RpcResponse get() {
        sync.acquire(-1);
        if (isDone()) {
            return this.response;
        }
        return null;
    }

    @Override
    public RpcResponse get(long timeout, TimeUnit unit) throws TimeoutException {
        try {
            boolean success = sync.tryAcquireNanos(-1, unit.toNanos(timeout));
            if (success) {
                if (isDone()) {
                    return this.response;
                } else {
                    return null;
                }
            } else {
                throw new TimeoutException();
            }
        } catch (TimeoutException e) {
            String exMsg = "Timeout exception. Request id: " + this.request.getRequestId()
                    + ". Request class name: " + this.request.getServiceName()
                    + ". Request method: " + this.request.getMethod();
            doneError(new RpcCallErrorException(exMsg, e));
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * future done
     */
    public void done(RpcResponse response) {
        if (isDone()) {
            return;
        }
        this.response = response;
        sync.release(1);
        RpcThreadPool.EXECUTORS.submit(() -> {
            for (AsyncRpcCallback callback : this.callbacks) {
                switch (response.getState()) {
                    case SUCCESS:
                    case RETRY:
                        callback.success(request, response);
                        break;
                    case ERROR:
                        callback.fail(request, new RpcCallErrorException("Response error", new Throwable(response.getInfo())));
                        break;
                    default:
                        break;
                }
            }
        });

        long responseTime = System.currentTimeMillis() - startTime;
        if (responseTime > this.responseTimeThreshold) {
            log.warn("service response time is too slow. Request id = '{}'. Response Time = {}ms", response.getRequestId(), responseTime);
        }
    }

    /**
     * 遇到异常, future done, reference端的异常
     */
    public void doneError(Exception e) {
        if (isDone()) {
            return;
        }
        sync.release(1);
        RpcThreadPool.EXECUTORS.submit(() -> {
            for (AsyncRpcCallback callback : this.callbacks) {
                callback.fail(request, e);
            }
        });

        long responseTime = System.currentTimeMillis() - startTime;
        if (responseTime > this.responseTimeThreshold) {
            log.warn("service response time is too slow. Request id = '{}'. Response Time = {}ms", response.getRequestId(), responseTime);
        }
    }

    /**
     * 添加callback
     */
    public RpcFuture addCallback(AsyncRpcCallback callback) {
        if (!this.isDone()) {
            this.callbacks.add(callback);
        }

        return this;
    }

    //getter
    public RpcRequest getRequest() {
        return request;
    }
}
