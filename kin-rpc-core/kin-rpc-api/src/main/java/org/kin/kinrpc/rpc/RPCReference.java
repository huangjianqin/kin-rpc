package org.kin.kinrpc.rpc;

import com.google.common.net.HostAndPort;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import org.kin.kinrpc.rpc.future.RPCFuture;
import org.kin.kinrpc.rpc.serializer.Serializer;
import org.kin.kinrpc.rpc.transport.ReferenceHandler;
import org.kin.kinrpc.rpc.transport.domain.RPCRequest;
import org.kin.kinrpc.rpc.transport.domain.RPCResponse;
import org.kin.transport.netty.core.ChannelExceptionHandler;
import org.kin.transport.netty.core.ClientTransportOption;
import org.kin.transport.netty.core.TransportOption;
import org.kin.transport.netty.core.listener.ChannelInactiveListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * Created by huangjianqin on 2019/6/14.
 */
public class RPCReference implements ChannelExceptionHandler, ChannelInactiveListener {
    private static final Logger log = LoggerFactory.getLogger(RPCReference.class);
    private volatile boolean isStopped;

    private Map<String, RPCFuture> pendingRPCFutureMap = new ConcurrentHashMap<>();

    private ClientTransportOption clientTransportOption;
    private ReferenceHandler connection;

    public RPCReference(InetSocketAddress address, Serializer serializer, int connectTimeout, boolean compression) {
        this.connection = new ReferenceHandler(address, serializer, this);
        this.clientTransportOption =
                TransportOption.client()
                        .channelOption(ChannelOption.TCP_NODELAY, true)
                        .channelOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                        .channelInactiveListener(this)
                        .channelExceptionHandler(this)
                        .protocolHandler(connection);
        if (compression) {
            clientTransportOption.compress();
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
            connection.request(request);
            pendingRPCFutureMap.put(request.getRequestId() + "", future);
        } catch (Exception e) {
            pendingRPCFutureMap.remove(request.getRequestId() + "");
            future.done(RPCResponse.respWithError(request, "client channel closed"));
        }

        return future;
    }

    private void clean() {
        for (RPCFuture rpcFuture : pendingRPCFutureMap.values()) {
            RPCRequest rpcRequest = rpcFuture.getRequest();
            RPCResponse rpcResponse = RPCResponse.respWithRetry(rpcRequest, "channel inactive");
            rpcFuture.done(rpcResponse);
        }
        this.pendingRPCFutureMap.clear();
    }

    public HostAndPort getAddress() {
        return HostAndPort.fromString(connection.getAddressStr());
    }

    public boolean isActive() {
        if (isStopped) {
            return false;
        }
        return connection.isActive();
    }

    public void start() {
        if (isStopped) {
            return;
        }
        connection.connect(clientTransportOption);
    }

    public void shutdown() {
        if (isStopped) {
            return;
        }
        isStopped = true;
        connection.close();
        clean();
    }

    /**
     * 已在pendingRPCFutureMap锁内执行
     */
    public void removeInvalid(RPCRequest rpcRequest) {
        this.pendingRPCFutureMap.remove(rpcRequest.getRequestId() + "");
    }

    /**
     * channel线程
     */
    @Override
    public void handleException(Channel channel, Throwable cause) {
        RPCThreadPool.THREADS.execute(() -> {
            clean();
        });
    }

    /**
     * channel线程
     */
    @Override
    public void channelInactive(Channel channel) {
        RPCThreadPool.THREADS.execute(() -> {
            clean();
            connection.connect(clientTransportOption);
        });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RPCReference that = (RPCReference) o;

        return connection != null ? connection.equals(that.connection) : that.connection == null;
    }

    @Override
    public int hashCode() {
        return connection != null ? connection.hashCode() : 0;
    }
}
