package org.kin.kinrpc.transport.kinrpc;


import org.kin.kinrpc.rpc.RpcThreadPool;
import org.kin.kinrpc.rpc.exception.RpcCallErrorException;
import org.kin.kinrpc.rpc.exception.RpcCallRetryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * kinrpc传输层记录每次rpc call request信息
 * Created by 健勤 on 2017/2/15.
 */
public class KinRpcInvocation {
    private static final Logger log = LoggerFactory.getLogger(KinRpcInvocation.class);
    private static final long RESPONSE_TIME_THRESHOLD = 5000;

    /** 用于记录服务调用的耗时(毫秒),衡量负载 */
    private final long startTime;
    /** kinrpc请求 */
    private final RpcRequest request;
    /** kinrpc响应 */
    private volatile RpcResponse response;
    /** async future入口 */
    private final CompletableFuture<Object> rootFuture = new CompletableFuture<>();
    /** 向外界暴露的future */
    private final CompletableFuture<Object> future;

    public KinRpcInvocation(RpcRequest request) {
        this.request = request;
        this.startTime = System.currentTimeMillis();
        //root future是由netty event loop complete, 故此处需要切换到RpcThreadPool.executors()线程处理
        future = rootFuture.thenApplyAsync(this::handleResult, RpcThreadPool.executors());
    }

    /**
     * 响应response
     */
    public void done(Object obj) {
        rootFuture.complete(obj);
    }

    /**
     * 是否完成
     */
    private boolean isDone() {
        return Objects.nonNull(response);
    }

    /**
     * 处理结果, 有可能是RpcResponse或者异常
     */
    private Object handleResult(Object obj) {
        if (isDone()) {
            if (Objects.nonNull(response)) {
                return response.getResult();
            } else {
                return null;
            }
        }

        long responseTime = System.currentTimeMillis() - startTime;
        if (responseTime > RESPONSE_TIME_THRESHOLD) {
            log.warn("service response time is too slow. Request id = '{}'. Response Time = {}ms", response.getRequestId(), responseTime);
        }

        if (obj instanceof RpcResponse) {
            return handleResponse((RpcResponse) obj);
        } else {
            return obj;
        }
    }

    /**
     * 处理RpcResponse响应逻辑
     */
    private Object handleResponse(RpcResponse response) {
        if (isDone()) {
            return response.getResult();
        }

        this.response = response;
        switch (response.getState()) {
            case SUCCESS:
                return response.getResult();
            case RETRY:
                throw new RpcCallRetryException(response.getInfo(), request.getServiceKey(), request.getMethod(), request.getParams());
            case ERROR:
                throw new RpcCallErrorException("Response error due to " + response.getInfo());
            default:
                throw new IllegalStateException("kinrpc response unknown state '".concat(response.getState().name()).concat("'"));
        }
    }

    //getter
    public RpcRequest getRequest() {
        return request;
    }

    public long getStartTime() {
        return startTime;
    }

    public RpcResponse getResponse() {
        return response;
    }

    public CompletableFuture<Object> getFuture() {
        return future;
    }
}
