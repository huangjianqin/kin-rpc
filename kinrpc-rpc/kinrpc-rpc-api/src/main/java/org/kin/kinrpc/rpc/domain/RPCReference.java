package org.kin.kinrpc.rpc.domain;

import com.google.common.net.HostAndPort;
import io.netty.channel.Channel;
import org.kin.kinrpc.rpc.future.RPCFuture;
import org.kin.kinrpc.rpc.serializer.Serializer;
import org.kin.kinrpc.rpc.transport.ReferenceHandler;
import org.kin.kinrpc.rpc.transport.domain.RPCRequest;
import org.kin.kinrpc.rpc.transport.domain.RPCResponse;
import org.kin.kinrpc.transport.listener.ChannelInactiveListener;
import org.kin.kinrpc.transport.listener.ExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * Created by huangjianqin on 2019/6/14.
 */
public class RPCReference implements ExceptionHandler, ChannelInactiveListener {
    private static final Logger log = LoggerFactory.getLogger("invoker");
    private Map<String, RPCFuture> pendingRPCFutureMap = new ConcurrentHashMap<String, RPCFuture>();
    private ReferenceHandler referenceHandler;

    public RPCReference(InetSocketAddress address) {
        this.referenceHandler = new ReferenceHandler(address, this);
    }

    public RPCReference(InetSocketAddress address, Serializer serializer) {
        this.referenceHandler = new ReferenceHandler(address, serializer, this);
    }

    public void handleResponse(RPCResponse rpcResponse) {
        String requestId = rpcResponse.getRequestId() + "";
        RPCFuture pendRPCFuture = pendingRPCFutureMap.get(requestId);
        if (pendRPCFuture != null) {
            pendingRPCFutureMap.remove(requestId);
            pendRPCFuture.done(rpcResponse);
        }
    }

    public Future<RPCResponse> request(RPCRequest request) throws Throwable {
        log.info("发送请求>>>" + request.toString());
        try {
            referenceHandler.request(request);
            RPCFuture future = new RPCFuture(request);
            pendingRPCFutureMap.put(request.getRequestId() + "", future);
            return future;
        } catch (Throwable e) {
            log.error("", e);
            throw e;
        }
    }

    private void clean() {
        for (RPCFuture rpcFuture : this.pendingRPCFutureMap.values()) {
            RPCRequest rpcRequest = rpcFuture.getRequest();
            RPCResponse rpcResponse = RPCResponse.respWithError(rpcRequest.getRequestId(),
                    rpcRequest.getServiceName(), rpcRequest.getMethod(), "channel inactive");
            rpcFuture.done(rpcResponse);
        }
        this.pendingRPCFutureMap.clear();

        shutdown();
    }

    public HostAndPort getAddress() {
        return HostAndPort.fromString(referenceHandler.getAddress());
    }

    public boolean isActive() {
        return referenceHandler.isActive();
    }

    public void start() {
        referenceHandler.connect();
    }

    public void shutdown() {
        referenceHandler.close();
    }

    @Override
    public void handleException(Channel channel, Throwable cause) {
        clean();
    }

    @Override
    public void channelInactive(Channel channel) {
        clean();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RPCReference that = (RPCReference) o;

        return referenceHandler != null ? referenceHandler.equals(that.referenceHandler) : that.referenceHandler == null;
    }

    @Override
    public int hashCode() {
        return referenceHandler != null ? referenceHandler.hashCode() : 0;
    }
}
