package org.kin.kinrpc.protocol;

import org.kin.framework.utils.ExtensionLoader;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.*;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.constants.ReferenceConstants;
import org.kin.kinrpc.constants.ServerAttachmentConstants;
import org.kin.kinrpc.transport.RemotingClient;
import org.kin.kinrpc.transport.cmd.RpcRequestCommand;
import org.kin.kinrpc.transport.cmd.RpcResponseCommand;
import org.kin.serialization.Serialization;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

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
    /** 服务端支持的序列化code */
    private final Byte serializationCode;

    public DefaultReferenceInvoker(ReferenceConfig<T> config,
                                   ServiceInstance instance,
                                   RemotingClient client) {
        this.instance = instance;
        this.client = client;
        String serialization = instance.metadata(ServiceMetadataConstants.SERIALIZATION_KEY);
        if (StringUtils.isBlank(serialization)) {
            serialization = config.getSerialization();
        }
        this.serializationCode = (byte) ExtensionLoader.getExtensionCode(Serialization.class, serialization);
    }

    @Override
    public RpcResult invoke(Invocation invocation) {
        Long timeout = invocation.attachment(ReferenceConstants.TIMEOUT_KEY, 0L);

        RpcRequestCommand command = new RpcRequestCommand(this.serializationCode, invocation.serviceId(),
                invocation.handlerId(), timeout, invocation.params());
        //bind server attachment
        Map<String, String> serverAttachments = invocation.getServerAttachments();
        addTokenIfExists(serverAttachments);
        command.setMetadata(serverAttachments);

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
                            resultFuture.completeExceptionally(new ServerErrorException(String.format("rpc call fail, due to %s,invocation=%s", response.getResult(), invocation)));
                        }
                    } catch (Exception e) {
                        resultFuture.completeExceptionally(new RpcException("rpc call fail, invocation=" + invocation, e));
                    }
                } else {
                    resultFuture.completeExceptionally(new RpcException("rpc call fail, invocation=" + invocation, t));
                }
            }, ReferenceContext.SCHEDULER);
        }

        return RpcResult.success(invocation, resultFuture);
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
}
