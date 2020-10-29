package org.kin.kinrpc.rpc;

import com.google.common.net.HostAndPort;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.kinrpc.rpc.future.RpcFuture;
import org.kin.kinrpc.rpc.transport.RpcRequest;
import org.kin.kinrpc.rpc.transport.RpcResponse;
import org.kin.kinrpc.transport.RpcEndpointRefHandler;
import org.kin.kinrpc.transport.protocol.RpcRequestProtocol;
import org.kin.kinrpc.transport.protocol.RpcResponseProtocol;
import org.kin.kinrpc.transport.serializer.Serializer;
import org.kin.kinrpc.transport.serializer.Serializers;
import org.kin.kinrpc.transport.serializer.UnknownSerializerException;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * Created by huangjianqin on 2019/6/14.
 */
public class RpcReference {
    private static final Logger log = LoggerFactory.getLogger(RpcReference.class);
    private volatile boolean isStopped;

    private Map<Long, RpcFuture> pendingRpcFutureMap = new ConcurrentHashMap<>();

    private String serviceName;
    private InetSocketAddress address;
    private Serializer serializer;
    private SocketClientTransportOption clientTransportOption;
    private ReferenceHandler referenceHandler;

    public RpcReference(String serviceName, InetSocketAddress address, Serializer serializer, int connectTimeout, CompressionType compressionType) {
        this.serviceName = serviceName;
        this.address = address;
        this.serializer = serializer;
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
     * 异常处理
     * channel线程
     */
    public void handleResponseError(long requestId, Throwable cause) {
        RpcFuture pendRpcFuture = pendingRpcFutureMap.get(requestId);
        if (pendRpcFuture != null) {
            pendRpcFuture.done(RpcResponse.respWithError(requestId, cause.getMessage()));
        }
    }

    /**
     * channel线程
     */
    public void handleResponse(RpcResponse rpcResponse) {
        rpcResponse.setHandleTime(System.currentTimeMillis());

        log.debug("receive a response >>> " + System.lineSeparator() + rpcResponse);

        long requestId = rpcResponse.getRequestId();
        RpcFuture pendRpcFuture = pendingRpcFutureMap.get(requestId);
        if (pendRpcFuture != null) {
            pendRpcFuture.done(rpcResponse);
        }
    }

    /**
     * 其他线程
     */
    public Future<RpcResponse> request(RpcRequest request) {
        RpcFuture future = new RpcFuture(request, this);
        if (!isActive()) {
            future.done(RpcResponse.respWithError(request, "client channel closed"));
            return future;
        }
        log.debug("send a request>>>" + System.lineSeparator() + request);

        try {
            pendingRpcFutureMap.put(request.getRequestId(), future);
            referenceHandler.request(request);
        } catch (Exception e) {
            onFail(request.getRequestId(), "client channel write error >>> ".concat(System.lineSeparator()).concat(ExceptionUtils.getExceptionDesc(e)));
        }

        return future;
    }

    public void clean() {
        for (RpcFuture rpcFuture : pendingRpcFutureMap.values()) {
            RpcRequest rpcRequest = rpcFuture.getRequest();
            RpcResponse rpcResponse = RpcResponse.respWithRetry(rpcRequest, "channel inactive");
            rpcFuture.done(rpcResponse);
        }
        this.pendingRpcFutureMap.clear();
    }

    public HostAndPort getAddress() {
        return HostAndPort.fromString(this.address.getHostName() + ":" + this.address.getPort());
    }

    public boolean isActive() {
        if (isStopped) {
            return false;
        }
        return referenceHandler.isActive();
    }

    public void start() {
        if (isStopped) {
            return;
        }
        referenceHandler.connect(clientTransportOption, address);
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
        this.pendingRpcFutureMap.remove(rpcRequest.getRequestId());
    }

    public void onFail(long requestId, String reason) {
        RpcFuture future = pendingRpcFutureMap.remove(requestId);
        if (Objects.nonNull(future)) {
            future.done(RpcResponse.respWithError(future.getRequest(), reason));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RpcReference that = (RpcReference) o;
        return serviceName.equals(that.serviceName) &&
                address.equals(that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceName, address);
    }

    //------------------------------------------------------------------------------------------------------------------
    private class ReferenceHandler extends RpcEndpointRefHandler {
        public void request(RpcRequest request) {
            if (isActive()) {
                try {
                    request.setCreateTime(System.currentTimeMillis());
                    byte[] data = serializer.serialize(request);

                    RpcRequestProtocol protocol = RpcRequestProtocol.create(request.getRequestId(), (byte) serializer.type(), data);
                    client.request(protocol, new ReferenceRequestListener(request.getRequestId()));

                    ProtocolStatisicService.instance().statisticReq(
                            request.getServiceName() + "-" + request.getMethod(), data.length
                    );
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                    onFail(request.getRequestId(), "client channel write error >>> ".concat(System.lineSeparator()).concat(ExceptionUtils.getExceptionDesc(e)));
                }
            }
        }

        @Override
        protected void handleRpcResponseProtocol(RpcResponseProtocol responseProtocol) {
            long requestId = responseProtocol.getRequestId();
            byte serializerType = responseProtocol.getSerializer();
            try {
                RpcResponse rpcResponse;
                try {
                    Serializer serializer = Serializers.getSerializer(serializerType);
                    if (Objects.isNull(serializer)) {
                        //未知序列化类型
                        throw new UnknownSerializerException(serializerType);
                    }

                    rpcResponse = serializer.deserialize(responseProtocol.getRespContent(), RpcResponse.class);
                    rpcResponse.setEventTime(System.currentTimeMillis());
                } catch (Exception e) {
                    handleResponseError(requestId, e);

                    log.error(e.getMessage(), e);
                    return;
                }

                ProtocolStatisicService.instance().statisticResp(
                        rpcResponse.getServiceName() + "-" + rpcResponse.getMethod(), responseProtocol.getRespContent().length
                );

                handleResponse(rpcResponse);
            } catch (Exception e) {
                log.error(e.getMessage(), e);

                throw new RuntimeException(e);
            }
        }

        @Override
        protected void connectionInactive() {
            RpcReference.this.clean();
        }
    }

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
