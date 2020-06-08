package org.kin.kinrpc.rpc;

import com.google.common.net.HostAndPort;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.kinrpc.rpc.future.RPCFuture;
import org.kin.kinrpc.rpc.transport.domain.RPCRequest;
import org.kin.kinrpc.rpc.transport.domain.RPCResponse;
import org.kin.kinrpc.rpc.transport.protocol.RPCRequestProtocol;
import org.kin.kinrpc.rpc.transport.protocol.RPCResponseProtocol;
import org.kin.kinrpc.serializer.Serializer;
import org.kin.transport.netty.core.Client;
import org.kin.transport.netty.core.ClientTransportOption;
import org.kin.transport.netty.core.TransportHandler;
import org.kin.transport.netty.core.TransportOption;
import org.kin.transport.netty.core.protocol.AbstractProtocol;
import org.kin.transport.netty.core.statistic.InOutBoundStatisicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by huangjianqin on 2019/6/14.
 */
public class RPCReference {
    private static final Logger log = LoggerFactory.getLogger(RPCReference.class);
    private volatile boolean isStopped;

    private Map<String, RPCFuture> pendingRPCFutureMap = new ConcurrentHashMap<>();

    private String serviceName;
    private InetSocketAddress address;
    private Serializer serializer;
    private ClientTransportOption clientTransportOption;
    private ReferenceHandler referenceHandler;

    public RPCReference(String serviceName, InetSocketAddress address, Serializer serializer, int connectTimeout, boolean compression) {
        this.serviceName = serviceName;
        this.address = address;
        this.serializer = serializer;
        this.referenceHandler = new ReferenceHandler();
        this.clientTransportOption =
                TransportOption.client()
                        .channelOption(ChannelOption.TCP_NODELAY, true)
                        .channelOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                        .channelOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                        //receive窗口缓存6mb
                        .channelOption(ChannelOption.SO_RCVBUF, 10 * 1024 * 1024)
                        //send窗口缓存64kb
                        .channelOption(ChannelOption.SO_SNDBUF, 64 * 1024)
                        .transportHandler(this.referenceHandler);

        if (compression) {
            this.clientTransportOption.compress();
        }
    }

    /**
     * channel线程
     */
    public void handleResponse(RPCResponse rpcResponse) {
        if (isStopped) {
            return;
        }
        rpcResponse.setHandleTime(System.currentTimeMillis());

        log.debug("receive a response >>> " + System.lineSeparator() + rpcResponse);

        String requestId = rpcResponse.getRequestId();
        RPCFuture pendRPCFuture = pendingRPCFutureMap.get(requestId);
        if (pendRPCFuture != null) {
            pendRPCFuture.done(rpcResponse);
        }
    }

    /**
     * 其他线程
     */
    public Future<RPCResponse> request(RPCRequest request) {
        RPCFuture future = new RPCFuture(request, this);
        if (!isActive()) {
            future.done(RPCResponse.respWithError(request, "client channel closed"));
            return future;
        }
        log.debug("send a request>>>" + System.lineSeparator() + request);

        try {
            pendingRPCFutureMap.put(request.getRequestId(), future);
            referenceHandler.request(request);
        } catch (Exception e) {
            onFail(request.getRequestId(), "client channel write error >>> ".concat(System.lineSeparator()).concat(ExceptionUtils.getExceptionDesc(e)));
        }

        return future;
    }

    public void clean() {
        for (RPCFuture rpcFuture : pendingRPCFutureMap.values()) {
            RPCRequest rpcRequest = rpcFuture.getRequest();
            RPCResponse rpcResponse = RPCResponse.respWithRetry(rpcRequest, "channel inactive");
            rpcFuture.done(rpcResponse);
        }
        this.pendingRPCFutureMap.clear();
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
        referenceHandler.connect(clientTransportOption);
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
     * 已在pendingRPCFutureMap锁内执行
     */
    public void removeInvalid(RPCRequest rpcRequest) {
        this.pendingRPCFutureMap.remove(rpcRequest.getRequestId());
    }

    private void onFail(String requestId, String reason) {
        RPCFuture future = pendingRPCFutureMap.remove(requestId);
        if (Objects.nonNull(future)) {
            future.done(RPCResponse.respWithError(future.getRequest(), reason));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RPCReference that = (RPCReference) o;
        return serviceName.equals(that.serviceName) &&
                address.equals(that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceName, address);
    }

    //------------------------------------------------------------------------------------------------------------------
    private class ReferenceHandler extends TransportHandler {
        private volatile Client client;
        private ClientTransportOption transportOption;

        public void connect(ClientTransportOption transportOption) {
            if (isStopped) {
                return;
            }
            if (isActive()) {
                return;
            }
            if (client != null) {
                client.close();
                client = null;
            }
            if (client == null) {
                try {
                    client = transportOption.tcp(address);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
                if (!isActive()) {
                    //n秒后重连
                    RPCThreadPool.EXECUTORS.schedule(() -> connect(transportOption), 5, TimeUnit.SECONDS);
                }
            }
        }

        public void close() {
            if (Objects.nonNull(client)) {
                client.close();
            }
        }

        public boolean isActive() {
            return !isStopped && client != null && client.isActive();
        }

        public void request(RPCRequest request) {
            if (isActive()) {
                try {
                    request.setCreateTime(System.currentTimeMillis());
                    byte[] data = serializer.serialize(request);

                    RPCRequestProtocol protocol = RPCRequestProtocol.create(data);
                    client.request(protocol, new ReferenceRequestListener(request.getRequestId()));

                    InOutBoundStatisicService.instance().statisticReq(
                            request.getServiceName() + "-" + request.getMethod(), data.length
                    );
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                    onFail(request.getRequestId(), "client channel write error >>> ".concat(System.lineSeparator()).concat(ExceptionUtils.getExceptionDesc(e)));
                }
            }
        }

        @Override
        public void handleProtocol(Channel channel, AbstractProtocol protocol) {
            if (!isActive()) {
                return;
            }
            if (Objects.isNull(protocol)) {
                return;
            }
            if (protocol instanceof RPCResponseProtocol) {
                RPCResponseProtocol responseProtocol = (RPCResponseProtocol) protocol;
                try {
                    RPCResponse rpcResponse;
                    try {
                        rpcResponse = serializer.deserialize(responseProtocol.getRespContent(), RPCResponse.class);
                        rpcResponse.setEventTime(System.currentTimeMillis());
                    } catch (IOException | ClassNotFoundException e) {
                        log.error(e.getMessage(), e);
                        return;
                    }

                    InOutBoundStatisicService.instance().statisticResp(
                            rpcResponse.getServiceName() + "-" + rpcResponse.getMethod(), responseProtocol.getRespContent().length
                    );

                    handleResponse(rpcResponse);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            } else {
                log.error("unknown protocol >>>> {}", protocol);
            }
        }

        @Override
        public void channelInactive(Channel channel) {
            RPCThreadPool.EXECUTORS.execute(() -> {
                RPCReference.this.clean();
                if (!isStopped) {
                    log.warn("reference({}, {}) reconnecting...", serviceName, getAddress());
                    connect(clientTransportOption);
                }
            });
        }
    }

    private class ReferenceRequestListener implements ChannelFutureListener {
        private String requestId;

        public ReferenceRequestListener(String requestId) {
            this.requestId = requestId;
        }

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (!future.isSuccess()) {
                onFail(requestId, "client channel write error");
            }
        }
    }
}
