package org.kin.kinrpc.rpc.future;


import org.kin.framework.concurrent.lock.OneLock;
import org.kin.kinrpc.rpc.RpcReference;
import org.kin.kinrpc.rpc.RpcThreadPool;
import org.kin.kinrpc.rpc.transport.RpcRequest;
import org.kin.kinrpc.rpc.transport.RpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
    private RpcReference rpcReference;

    public RpcFuture(RpcRequest request, RpcReference rpcReference) {
        this.sync = new OneLock();
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
    public RpcResponse get() {
        sync.acquire(-1);
        if (isDone()) {
            return this.response;
        }
        return null;
    }

    @Override
    public RpcResponse get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
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
        RpcResponse rpcResponse = RpcResponse.respWithError(request, getTimeoutMessage());
        done(rpcResponse);
    }

    public void done(RpcResponse response) {
        if (isDone()) {
            return;
        }
        this.response = response;
        rpcReference.removeInvalid(request);
        sync.release(1);
        RpcThreadPool.EXECUTORS.submit(() -> {
            for (AsyncRpcCallback callback : this.callbacks) {
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
            log.warn("service response time is too slow. Request id = '{}'. Response Time = {}ms", response.getRequestId(), responseTime);
        }
    }

    public RpcFuture addRpcCallback(AsyncRpcCallback callback) {
        if (!this.isDone()) {
            this.callbacks.add(callback);
        }

        return this;
    }

    public RpcRequest getRequest() {
        return request;
    }
}
