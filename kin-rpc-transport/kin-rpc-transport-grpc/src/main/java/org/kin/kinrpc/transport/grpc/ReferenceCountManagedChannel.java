package org.kin.kinrpc.transport.grpc;

import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 可复用的grpc channel
 *
 * @author huangjianqin
 * @date 2020/12/2
 */
public class ReferenceCountManagedChannel extends ManagedChannel {
    /** 引用数 */
    private final AtomicInteger referenceCount = new AtomicInteger(0);
    /** grpc channel */
    private final ManagedChannel grpcChannel;

    public ReferenceCountManagedChannel(ManagedChannel delegated) {
        this.grpcChannel = delegated;
    }

    /**
     * The reference count of current ExchangeClient, connection will be closed if all invokers destroyed.
     */
    public void incrementAndGetCount() {
        referenceCount.incrementAndGet();
    }

    @Override
    public ManagedChannel shutdown() {
        if (referenceCount.decrementAndGet() <= 0) {
            return grpcChannel.shutdown();
        }
        return grpcChannel;
    }

    @Override
    public boolean isShutdown() {
        return grpcChannel.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return grpcChannel.isTerminated();
    }

    @Override
    public ManagedChannel shutdownNow() {
        return shutdown();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return grpcChannel.awaitTermination(timeout, unit);
    }

    @Override
    public <RequestT, ResponseT> ClientCall<RequestT, ResponseT> newCall(MethodDescriptor<RequestT, ResponseT> methodDescriptor, CallOptions callOptions) {
        return grpcChannel.newCall(methodDescriptor, callOptions);
    }

    @Override
    public String authority() {
        return grpcChannel.authority();
    }
}
