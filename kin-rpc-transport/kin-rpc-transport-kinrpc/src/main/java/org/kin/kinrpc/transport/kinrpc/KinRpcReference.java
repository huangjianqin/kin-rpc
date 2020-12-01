package org.kin.kinrpc.transport.kinrpc;

import com.google.common.base.Preconditions;
import com.google.common.net.HostAndPort;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.kinrpc.rpc.RpcThreadPool;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.rpc.exception.RpcCallErrorException;
import org.kin.kinrpc.serializer.Serializer;
import org.kin.kinrpc.serializer.SerializerType;
import org.kin.kinrpc.serializer.Serializers;
import org.kin.kinrpc.serializer.UnknownSerializerException;
import org.kin.transport.netty.CompressionType;
import org.kin.transport.netty.Transports;
import org.kin.transport.netty.socket.client.SocketClientTransportOption;
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
    private final SocketClientTransportOption clientTransportOption;
    private final ReferenceHandler referenceHandler;
    private final Serializer serializer;

    public KinRpcReference(Url url) {
        this.url = url;
        int connectTimeout = Integer.parseInt(url.getNumberParam(Constants.CONNECT_TIMEOUT_KEY));
        int compression = Integer.parseInt(url.getNumberParam(Constants.COMPRESSION_KEY));

        int serializerType = Integer.parseInt(url.getNumberParam(Constants.SERIALIZE_KEY));
        if (serializerType == 0) {
            //未指定序列化类型, 默认kyro
            serializerType = SerializerType.KRYO.getCode();
        }
        //先校验, 顺便初始化
        Preconditions.checkNotNull(Serializers.getSerializer(serializerType), "unvalid serializer type: [" + serializerType + "]");

        CompressionType compressionType = CompressionType.getById(compression);
        Preconditions.checkNotNull(compressionType, "unvalid compression type: id=" + compression + "");

        this.serializer = Serializers.getSerializer(serializerType);
        this.referenceHandler = new ReferenceHandler();
        this.clientTransportOption =
                Transports.socket().client()
                        .channelOption(ChannelOption.TCP_NODELAY, true)
                        .channelOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                        .channelOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                        //receive窗口缓存6mb
                        .channelOption(ChannelOption.SO_RCVBUF, 10 * 1024 * 1024)
                        //send窗口缓存64kb
                        .channelOption(ChannelOption.SO_SNDBUF, 64 * 1024)
                        .protocolHandler(this.referenceHandler)
                        .compress(compressionType)
                        .build();
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
        CompletableFuture<Object> future = invocation.getFuture().thenApplyAsync(obj -> {
            removeInvalid(request);
            //此处才抛出异常, 因为KinRpcInvocation内部需要记录一下信息
            if (obj instanceof Throwable) {
                throw new RpcCallErrorException("", (Throwable) obj);
            }
            //返回服务接口结果
            return obj;
        }, RpcThreadPool.executors());
        if (!isActive()) {
            invocation.done(new RpcCallErrorException("client channel closed"));
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

    public void clean() {
        for (KinRpcInvocation rpcFuture : invocations.values()) {
            RpcRequest rpcRequest = rpcFuture.getRequest();
            RpcResponse rpcResponse = RpcResponse.respWithRetry(rpcRequest, "channel inactive");
            rpcFuture.done(rpcResponse);
        }
        this.invocations.clear();
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

    public void shutdown() {
        if (isStopped) {
            return;
        }
        isStopped = true;
        referenceHandler.close();
        clean();
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
    private class ReferenceHandler extends KinRpcEndpointRefHandler {
        public void request(RpcRequest request) {
            if (isActive()) {
                try {
                    request.setCreateTime(System.currentTimeMillis());
                    byte[] data = serializer.serialize(request);

                    KinRpcRequestProtocol protocol = KinRpcRequestProtocol.create(request.getRequestId(), (byte) serializer.type(), data);
                    client.request(protocol, new ReferenceRequestListener(request.getRequestId()));

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
            byte serializerType = responseProtocol.getSerializer();
            try {
                RpcResponse rpcResponse;
                byte[] respContent = responseProtocol.getRespContent();
                try {
                    Serializer serializer = Serializers.getSerializer(serializerType);
                    if (Objects.isNull(serializer)) {
                        //未知序列化类型
                        throw new UnknownSerializerException(serializerType);
                    }

                    rpcResponse = serializer.deserialize(respContent, RpcResponse.class);
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
                log.error(e.getMessage(), e);

                throw new RpcCallErrorException(e);
            }
        }

        @Override
        protected void connectionInactive() {
            KinRpcReference.this.clean();
        }
    }

    /**
     * 用于reference发送失败时, 及时作出响应
     */
    private class ReferenceRequestListener implements ChannelFutureListener {
        private long requestId;

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
