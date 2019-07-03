package org.kin.kinrpc.rpc;

import com.google.common.net.HostAndPort;
import io.netty.channel.Channel;
import org.kin.framework.concurrent.ThreadManager;
import org.kin.kinrpc.rpc.future.RPCFuture;
import org.kin.kinrpc.rpc.serializer.Serializer;
import org.kin.kinrpc.rpc.transport.ReferenceHandler;
import org.kin.kinrpc.rpc.transport.domain.RPCRequest;
import org.kin.kinrpc.rpc.transport.domain.RPCResponse;
import org.kin.kinrpc.transport.ChannelExceptionHandler;
import org.kin.kinrpc.transport.listener.ChannelInactiveListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * Created by huangjianqin on 2019/6/14.
 */
public class RPCReference implements ChannelExceptionHandler, ChannelInactiveListener {
    private static final Logger log = LoggerFactory.getLogger("invoker");
    private Map<String, RPCFuture> pendingRPCFutureMap = new ConcurrentHashMap<String, RPCFuture>();
    private ReferenceHandler connection;
    private volatile boolean isStopped;

    public RPCReference(InetSocketAddress address, Serializer serializer, int connectTimeout) {
        this.connection = new ReferenceHandler(address, serializer, this, connectTimeout);
    }

    public void handleResponse(RPCResponse rpcResponse) {
        if(isStopped){
            return;
        }
        log.debug("revceive a response >>> " + System.lineSeparator() + rpcResponse);

        rpcResponse.setHandleTime(System.currentTimeMillis());
        String requestId = rpcResponse.getRequestId() + "";
        RPCFuture pendRPCFuture = pendingRPCFutureMap.get(requestId);
        if (pendRPCFuture != null) {
            pendRPCFuture.done(rpcResponse);
        }
    }

    public Future<RPCResponse> request(RPCRequest request){
        RPCFuture future = new RPCFuture(request, this);
        if(isStopped){
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
        Collection<RPCFuture> copy = new ArrayList<>(this.pendingRPCFutureMap.values());
        for (RPCFuture rpcFuture : copy) {
            RPCRequest rpcRequest = rpcFuture.getRequest();
            RPCResponse rpcResponse = RPCResponse.respWithRetry(rpcRequest, "channel inactive");
            rpcFuture.done(rpcResponse);
        }
        this.pendingRPCFutureMap.clear();
    }

    public HostAndPort getAddress() {
        return HostAndPort.fromString(connection.getAddress());
    }

    public boolean isActive() {
        if(isStopped){
            return false;
        }
        return connection.isActive();
    }

    public void start() {
        if(isStopped){
            return;
        }
        connection.connect();
    }

    public void shutdown() {
        if(isStopped){
            return;
        }
        isStopped = true;
        connection.close();
    }

    public void removeInvalid(RPCRequest rpcRequest) {
        this.pendingRPCFutureMap.remove(rpcRequest.getRequestId() + "");
    }

    public void setRate(double rate){
        connection.setRate(rate);
    }

    @Override
    public void handleException(Channel channel, Throwable cause) {
        clean();
    }

    @Override
    public void channelInactive(Channel channel) {
        clean();
        try {
            ThreadManager.DEFAULT.execute(() -> {
                connection.connect();
            });
        } catch (Exception e) {

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

        RPCReference that = (RPCReference) o;

        return connection != null ? connection.equals(that.connection) : that.connection == null;
    }

    @Override
    public int hashCode() {
        return connection != null ? connection.hashCode() : 0;
    }
}