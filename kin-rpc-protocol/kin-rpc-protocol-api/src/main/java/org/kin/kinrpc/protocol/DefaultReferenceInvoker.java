package org.kin.kinrpc.protocol;

import org.kin.kinrpc.*;
import org.kin.kinrpc.transport.RemotingClient;
import org.kin.kinrpc.transport.cmd.RpcRequestCommand;
import org.kin.kinrpc.transport.cmd.RpcResponseCommand;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * @author huangjianqin
 * @date 2023/6/28
 */
public class DefaultReferenceInvoker<T> implements ReferenceInvoker<T> {
    /** service instance */
    private final ServiceInstance instance;
    /** remoting client */
    private final RemotingClient client;

    public DefaultReferenceInvoker(ServiceInstance instance,
                                   RemotingClient client) {
        this.instance = instance;
        this.client = client;
    }

    @Override
    public RpcResult invoke(Invocation invocation) {
        RpcRequestCommand command = new RpcRequestCommand(invocation.serializationCode(), invocation.serviceId(),
                invocation.handlerId(), invocation.params());

        CompletableFuture<Object> resultFuture = new CompletableFuture<>();
        if (invocation.isVoid()) {
            CompletableFuture<Void> completeSignal = client.fireAndForget(command);
            completeSignal.whenCompleteAsync((response, t) -> {
                if (Objects.isNull(t)) {
                    //send rpc call success
                    resultFuture.complete(null);
                } else {
                    resultFuture.completeExceptionally(new RpcException("rpc call fail, invocation=" + invocation, t));
                }
            });
        } else {
            CompletableFuture<RpcResponseCommand> respFuture = client.requestResponse(command);

            respFuture.whenCompleteAsync((response, t) -> {
                if (Objects.isNull(t)) {
                    //send rpc call success
                    try {
                        if (response.isOk()) {
                            response.deserializeResult(invocation.realReturnType());
                            resultFuture.complete(response.getResult());
                        } else {
                            response.deserializeResult(String.class);
                            resultFuture.completeExceptionally(new RpcBizException(String.format("rpc call fail, due to %s,invocation=%s", response.getResult(), invocation)));
                        }
                    } catch (Exception e) {
                        resultFuture.completeExceptionally(new RpcException("rpc call fail, invocation=" + invocation, t));
                    }
                } else {
                    resultFuture.completeExceptionally(new RpcException("rpc call fail, invocation=" + invocation, t));
                }
            }, ReferenceContext.EXECUTOR);
        }

        return RpcResult.success(invocation, resultFuture);
    }

    @Override
    public ServiceInstance serviceInstance() {
        return instance;
    }

    @Override
    public boolean isAvailable() {
        return client.isAvailable();
    }

    @Override
    public void destroy() {
        client.shutdown();
    }
}
