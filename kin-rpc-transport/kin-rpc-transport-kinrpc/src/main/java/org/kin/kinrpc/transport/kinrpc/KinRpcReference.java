package org.kin.kinrpc.transport.kinrpc;

import com.google.common.base.Preconditions;
import com.google.common.net.HostAndPort;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.SslConfig;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.rpc.exception.RpcCallErrorException;
import org.kin.kinrpc.rpc.exception.RpcCallRetryException;
import org.kin.kinrpc.serialization.Serialization;
import org.kin.kinrpc.serialization.SerializationType;
import org.kin.kinrpc.serialization.Serializations;
import org.kin.kinrpc.serialization.UnknownSerializationException;
import org.kin.kinrpc.transport.NettyUtils;
import org.kin.transport.netty.CompressionType;
import org.kin.transport.netty.Transports;
import org.kin.transport.netty.socket.SocketTransportOption;
import org.kin.transport.netty.socket.protocol.ProtocolStatisicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by huangjianqin on 2019/6/14.
 */
public class KinRpcReference {
    private static final Logger log = LoggerFactory.getLogger(KinRpcReference.class);
    private volatile boolean isStopped;
    /** 异步rpc call future */
    private Map<Long, KinRpcInvocation> invocations = new ConcurrentHashMap<>();

    private final Url url;
    private final SocketTransportOption clientTransportOption;
    private final ReferenceHandler referenceHandler;
    private final Serialization serialization;

    public KinRpcReference(Url url) {
        this.url = url;
        int compression = url.getIntParam(Constants.COMPRESSION_KEY);

        int serializationType = url.getIntParam(Constants.SERIALIZATION_KEY);
        if (serializationType == 0) {
            //未指定序列化类型, 默认kyro
            serializationType = SerializationType.KRYO.getCode();
        }
        //先校验, 顺便初始化
        Preconditions.checkNotNull(Serializations.getSerialization(serializationType), "unvalid serialization type: [" + serializationType + "]");

        CompressionType compressionType = CompressionType.getById(compression);
        Preconditions.checkNotNull(compressionType, "unvalid compression type: id=" + compression + "");

        this.serialization = Serializations.getSerialization(serializationType);
        this.referenceHandler = new ReferenceHandler();

        SocketTransportOption.SocketClientTransportOptionBuilder builder = Transports.socket().client()
                .channelOptions(NettyUtils.convert(url))
                .protocolHandler(this.referenceHandler)
                .compress(compressionType);

        String certPath = SslConfig.INSTANCE.getClientKeyCertChainPath();
        String keyPath = SslConfig.INSTANCE.getClientPrivateKeyPath();

        if (StringUtils.isNotBlank(certPath) && StringUtils.isNotBlank(keyPath)) {
            builder.ssl(certPath, keyPath);
        }

        this.clientTransportOption = builder.build();
    }

    /**
     * channel线程
     */
    public void handleResponse(RpcResponse rpcResponse) {
        rpcResponse.setHandleTime(System.currentTimeMillis());

        log.debug("receive a response >>> " + System.lineSeparator() + rpcResponse);

        long requestId = rpcResponse.getRequestId();
        KinRpcInvocation invocation = invocations.get(requestId);
        if (invocation != null) {
            invocation.done(rpcResponse);
        }
    }

    /**
     * 其他线程
     */
    public CompletableFuture<Object> request(RpcRequest request) {
        KinRpcInvocation invocation = new KinRpcInvocation(request);
        CompletableFuture<Object> future = invocation.getFuture().thenApply(obj -> {
            removeInvalid(request);
            //此处才抛出异常, 因为KinRpcInvocation内部需要记录一下信息
            if (obj instanceof Throwable) {
                //rpc call error
                if (obj instanceof RpcCallRetryException) {
                    throw (RpcCallRetryException) obj;
                } else {
                    //封装成RpcCallRetryException
                    throw new RpcCallRetryException((Throwable) obj);
                }
            }
            //返回服务接口结果
            return obj;
        });
        if (!isActive()) {
            invocation.done(new IllegalStateException("client channel closed"));
            return future;
        }
        log.debug("send a request>>>" + System.lineSeparator() + request);

        try {
            invocations.put(request.getRequestId(), invocation);
            referenceHandler.request(request);
        } catch (Exception e) {
            onFail(request.getRequestId(), "client channel write error >>> ".concat(System.lineSeparator()).concat(ExceptionUtils.getExceptionDesc(e)));
        }

        return future;
    }

    public HostAndPort getAddress() {
        return HostAndPort.fromString(url.getAddress());
    }

    public boolean isActive() {
        if (isStopped) {
            return false;
        }
        return referenceHandler.isActive();
    }

    /**
     * 连接remote server
     */
    public void connect() {
        if (isStopped) {
            return;
        }
        HostAndPort hostAndPort = getAddress();
        referenceHandler.connect(clientTransportOption, new InetSocketAddress(hostAndPort.getHost(), hostAndPort.getPort()));
    }

    /**
     * close connection
     */
    public void shutdown() {
        if (isStopped) {
            return;
        }
        isStopped = true;
        referenceHandler.close();

        //以error形式complete future
        for (KinRpcInvocation rpcFuture : invocations.values()) {
            RpcRequest rpcRequest = rpcFuture.getRequest();
            RpcResponse rpcResponse = RpcResponse.respWithError(rpcRequest, "connection closed");
            rpcFuture.done(rpcResponse);
        }
        this.invocations.clear();
    }

    /**
     * 已在pendingRpcFutureMap锁内执行
     */
    public void removeInvalid(RpcRequest rpcRequest) {
        this.invocations.remove(rpcRequest.getRequestId());
    }

    /**
     * rpc call fail
     */
    public void onFail(long requestId, String reason) {
        KinRpcInvocation invocation = invocations.remove(requestId);
        if (Objects.nonNull(invocation)) {
            invocation.done(new RpcCallErrorException(reason));
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * 传输层逻辑处理实现类
     */
    private class ReferenceHandler extends KinRpcEndpointRefHandler {
        public void request(RpcRequest request) {
            if (isActive()) {
                try {
                    byte[] data = serialization.serialize(request);

                    KinRpcRequestProtocol protocol = KinRpcRequestProtocol.create(request.getRequestId(), (byte) serialization.type(), data);
                    client.sendAndFlush(protocol, new ReferenceRequestListener(request.getRequestId()));

                    ProtocolStatisicService.instance().statisticReq(
                            request.getServiceKey() + "-" + request.getMethod(), data.length
                    );
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                    onFail(request.getRequestId(), "client channel write error >>> ".concat(System.lineSeparator()).concat(ExceptionUtils.getExceptionDesc(e)));
                }
            }
        }

        @Override
        protected void handleRpcResponseProtocol(KinRpcResponseProtocol responseProtocol) {
            long requestId = responseProtocol.getRequestId();
            byte serializationType = responseProtocol.getSerialization();
            try {
                RpcResponse rpcResponse;
                byte[] respContent = responseProtocol.getRespContent();
                try {
                    Serialization serialization = Serializations.getSerialization(serializationType);
                    if (Objects.isNull(serialization)) {
                        //未知序列化类型
                        throw new UnknownSerializationException(serializationType);
                    }

                    rpcResponse = serialization.deserialize(respContent, RpcResponse.class);
                    rpcResponse.setEventTime(System.currentTimeMillis());
                } catch (Exception e) {
                    onFail(requestId, e.getMessage());

                    log.error(e.getMessage(), e);
                    return;
                }

                ProtocolStatisicService.instance().statisticResp(
                        rpcResponse.getServiceKey() + "-" + rpcResponse.getMethod(), Objects.nonNull(respContent) ? respContent.length : 0
                );

                handleResponse(rpcResponse);
            } catch (Exception e) {
                ExceptionUtils.throwExt(e);
            }
        }

        @Override
        protected void connectionInactive() {
            //以retry形式complete future
            for (KinRpcInvocation rpcFuture : invocations.values()) {
                RpcRequest rpcRequest = rpcFuture.getRequest();
                RpcResponse rpcResponse = RpcResponse.respWithRetry(rpcRequest, "channel inactive");
                rpcFuture.done(rpcResponse);
            }
            invocations.clear();
        }
    }

    /**
     * 用于reference发送失败时, 及时作出响应
     */
    private class ReferenceRequestListener implements ChannelFutureListener {
        /** rpc request uuid */
        private final long requestId;

        public ReferenceRequestListener(long requestId) {
            this.requestId = requestId;
        }

        @Override
        public void operationComplete(ChannelFuture future) {
            if (!future.isSuccess()) {
                onFail(requestId, "client channel write error");
            }
        }
    }
}
