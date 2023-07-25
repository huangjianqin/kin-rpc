package org.kin.kinrpc.protocol;

import org.kin.framework.concurrent.ThreadLessExecutor;
import org.kin.framework.utils.ExtensionLoader;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.*;
import org.kin.kinrpc.config.MethodConfig;
import org.kin.kinrpc.constants.ReferenceConstants;
import org.kin.kinrpc.constants.ServerAttachmentConstants;
import org.kin.kinrpc.transport.RemotingClient;
import org.kin.kinrpc.transport.cmd.RpcRequestCommand;
import org.kin.kinrpc.transport.cmd.RpcResponseCommand;
import org.kin.serialization.Serialization;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 默认{@link ReferenceInvoker}实现, 基于{@link RemotingClient}发起服务调用
 *
 * @author huangjianqin
 * @date 2023/6/28
 */
public class DefaultReferenceInvoker<T> implements ReferenceInvoker<T> {
    /** service instance */
    private final ServiceInstance instance;
    /** remoting client */
    private final RemotingClient client;
    /** 服务支持的序列化code */
    private final Byte serializationCode;

    public DefaultReferenceInvoker(ServiceInstance instance,
                                   RemotingClient client) {
        this.instance = instance;
        this.client = client;
        String serialization = instance.metadata(ServiceMetadataConstants.SERIALIZATION_KEY);
        if (StringUtils.isNotBlank(serialization)) {
            this.serializationCode = (byte) ExtensionLoader.getExtensionCode(Serialization.class, serialization);
        } else {
            this.serializationCode = null;
        }
    }

    @Override
    public RpcResult invoke(Invocation invocation) {
        MethodConfig methodConfig = invocation.attachment(ReferenceConstants.METHOD_CONFIG_KEY);
        boolean asyncInvoke = invocation.isAsyncReturn();
        int timeoutMs = 0;
        if (Objects.nonNull(methodConfig)) {
            asyncInvoke |= methodConfig.isAsync();
            timeoutMs = methodConfig.getTimeout();
        }

        Byte serializationCode = this.serializationCode;
        if (Objects.isNull(serializationCode)) {
            String serialization = invocation.attachment(ReferenceConstants.SERIALIZATION_KEY);
            if (StringUtils.isNotBlank(serialization)) {
                serializationCode = (byte) ExtensionLoader.getExtensionCode(Serialization.class, serialization);
            }
        }

        if (Objects.isNull(serializationCode)) {
            return RpcResult.fail(invocation,
                    new RpcException("rpc call fail, due to can not find available serialization, invocation=" + invocation));
        }

        RpcRequestCommand command = new RpcRequestCommand(this.serializationCode, invocation.serviceId(),
                invocation.handlerId(), timeoutMs > 0 ? System.currentTimeMillis() + timeoutMs : 0, invocation.params());
        //bind server attachment
        Map<String, String> serverAttachments = invocation.serverAttachments();
        addTokenIfExists(serverAttachments);
        command.setMetadata(serverAttachments);

        return asyncInvoke ? asyncInvoke(invocation, command, timeoutMs) : invoke(invocation, command, timeoutMs);
    }

    /**
     * sync rpc call
     *
     * @param invocation rpc call信息
     * @param command    rpc request command
     * @param timeoutMs  rpc call timeout
     * @return rpc result
     */
    private RpcResult invoke(Invocation invocation,
                             RpcRequestCommand command,
                             int timeoutMs) {
        CompletableFuture<Object> resultFuture = new CompletableFuture<>();
        //相当于callback
        ThreadLessExecutor threadLessExecutor = new ThreadLessExecutor(resultFuture);

        Future<?> timeoutFuture;
        if (timeoutMs > 0) {
            //async timeout
            //这里没有统一纳管timeoutFuture, 一般超时时间都比较短, 待调度触发完成后, 有jvm回收
            timeoutFuture = ReferenceContext.SCHEDULER.schedule(() -> {
                if (!threadLessExecutor.isWaiting()) {
                    //服务调用已有返回结果
                    return;
                }

                threadLessExecutor.notifyExceptionally(new RpcTimeoutException(invocation, timeoutMs));
            }, timeoutMs, TimeUnit.MILLISECONDS);
        } else {
            timeoutFuture = null;
        }

        if (invocation.isVoid()) {
            CompletableFuture<Void> completeSignal = client.fireAndForget(command);
            completeSignal.whenComplete((response, t) ->
                    threadLessExecutor.execute(() ->
                            onFireAndForgetCompleted(invocation, resultFuture, timeoutFuture, t)));
        } else {
            CompletableFuture<RpcResponseCommand> respFuture = client.requestResponse(command);

            respFuture.whenComplete((response, t) ->
                    threadLessExecutor.execute(() ->
                            onRequestAndResponseCompleted(invocation, resultFuture, timeoutFuture, response, t)));
        }

        //block and wait return
        try {
            threadLessExecutor.waitAndDrain();
        } catch (InterruptedException e) {
            resultFuture.completeExceptionally(e);
        }
        return RpcResult.success(invocation, resultFuture);
    }

    /**
     * async rpc call
     *
     * @param invocation rpc call信息
     * @param command    rpc request command
     * @param timeoutMs  rpc call timeout
     * @return rpc result
     */
    private RpcResult asyncInvoke(Invocation invocation,
                                  RpcRequestCommand command,
                                  int timeoutMs) {
        CompletableFuture<Object> resultFuture = new CompletableFuture<>();
        Future<?> timeoutFuture;
        if (timeoutMs > 0) {
            //async timeout
            //这里没有统一纳管timeoutFuture, 一般超时时间都比较短, 待调度触发完成后, 有jvm回收
            timeoutFuture = ReferenceContext.SCHEDULER.schedule(() -> {
                if (resultFuture.isDone()) {
                    //服务调用已有返回结果
                    return;
                }

                resultFuture.completeExceptionally(new RpcTimeoutException(invocation, timeoutMs));
            }, timeoutMs, TimeUnit.MILLISECONDS);
        } else {
            timeoutFuture = null;
        }

        if (invocation.isVoid()) {
            CompletableFuture<Void> completeSignal = client.fireAndForget(command);
            completeSignal.whenCompleteAsync((response, t) ->
                            onFireAndForgetCompleted(invocation, resultFuture, timeoutFuture, t),
                    ReferenceContext.SCHEDULER);
        } else {
            CompletableFuture<RpcResponseCommand> respFuture = client.requestResponse(command);

            respFuture.whenCompleteAsync((response, t) ->
                            onRequestAndResponseCompleted(invocation, resultFuture, timeoutFuture, response, t),
                    ReferenceContext.SCHEDULER);
        }

        return RpcResult.success(invocation, resultFuture);
    }

    /**
     * operation after fire and forget completed
     *
     * @param invocation    rpc call信息
     * @param resultFuture  result future
     * @param timeoutFuture rpc call timeout future
     * @param t             rpc call异常
     */
    private void onFireAndForgetCompleted(@Nonnull Invocation invocation,
                                          @Nonnull CompletableFuture<Object> resultFuture,
                                          @Nullable Future<?> timeoutFuture,
                                          Throwable t) {
        if (Objects.nonNull(timeoutFuture) && !timeoutFuture.isDone()) {
            timeoutFuture.cancel(true);
        }

        if (resultFuture.isDone()) {
            return;
        }

        if (Objects.isNull(t)) {
            //send rpc call success
            resultFuture.complete(null);
        } else {
            resultFuture.completeExceptionally(new RpcException("rpc call fail, invocation=" + invocation, t));
        }
    }

    /**
     * operation after request response
     *
     * @param invocation    rpc call信息
     * @param resultFuture  result future
     * @param timeoutFuture rpc call timeout future
     * @param response      rpc response
     * @param t             rpc call异常
     */
    private void onRequestAndResponseCompleted(@Nonnull Invocation invocation,
                                               @Nonnull CompletableFuture<Object> resultFuture,
                                               @Nullable Future<?> timeoutFuture,
                                               RpcResponseCommand response,
                                               Throwable t) {
        if (Objects.nonNull(timeoutFuture) && !timeoutFuture.isDone()) {
            timeoutFuture.cancel(true);
        }

        if (resultFuture.isDone()) {
            return;
        }

        if (Objects.isNull(t)) {
            //send rpc call success
            try {
                if (response.isOk()) {
                    response.deserializeResult(invocation.realReturnType());
                    resultFuture.complete(response.getResult());
                } else {
                    response.deserializeResult(String.class);
                    resultFuture.completeExceptionally(new ServerErrorException(String.format("rpc call fail, due to %s,invocation=%s", response.getResult(), invocation)));
                }
            } catch (Exception e) {
                resultFuture.completeExceptionally(new RpcException("rpc call fail, invocation=" + invocation, e));
            }
        } else {
            resultFuture.completeExceptionally(new RpcException("rpc call fail, invocation=" + invocation, t));
        }
    }

    /**
     * 将token信息绑定到server attachment
     *
     * @param serverAttachments server attachment
     */
    private void addTokenIfExists(Map<String, String> serverAttachments) {
        String token = instance.metadata(ServiceMetadataConstants.TOKEN_KEY);
        if (StringUtils.isBlank(token)) {
            return;
        }

        serverAttachments.put(ServerAttachmentConstants.TOKEN_KEY, token);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultReferenceInvoker<?> that = (DefaultReferenceInvoker<?>) o;
        return Objects.equals(instance, that.instance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instance);
    }

    @Override
    public String toString() {
        return "DefaultReferenceInvoker{" +
                "instance=" + instance +
                ", serializationCode=" + serializationCode +
                '}';
    }
}
