package org.kin.kinrpc.transport;

import org.jctools.maps.NonBlockingHashMap;
import org.kin.kinrpc.transport.cmd.RemotingCodec;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * @author huangjianqin
 * @date 2023/6/7
 */
public abstract class AbsRemotingClient implements RemotingClient {
    /** remoting codec */
    protected final RemotingCodec codec = new RemotingCodec();
    /** remoting processor */
    protected final RemotingProcessor remotingProcessor = new RemotingProcessor(codec);
    /** key -> request id, value -> request future */
    protected final Map<Long, CompletableFuture<Object>> requestFutureMap = new NonBlockingHashMap<>();
    /** remote host */
    protected final String host;
    /** remote port */
    protected final int port;
    /** remote address */
    protected final InetSocketAddress remoteAddress;
    /** client端默认{@link ChannelContext}实现 */
    protected final ChannelContext clientChannelContext = new ChannelContext() {
        @Override
        public SocketAddress address() {
            return remoteAddress;
        }

        @Nullable
        @Override
        public CompletableFuture<Object> removeRequestFuture(long requestId) {
            return AbsRemotingClient.this.removeRequestFuture(requestId);
        }
    };

    protected AbsRemotingClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.remoteAddress =new InetSocketAddress(host, port);
    }

    /**
     * 根据{@code requestId}创建request future
     * @return  request future
     */
    protected CompletableFuture<Object> createRequestFuture(long requestId) {
        CompletableFuture<Object> requestFuture = new CompletableFuture<>();
        CompletableFuture<Object> current = requestFutureMap.computeIfAbsent(requestId, i -> requestFuture);
        if (current != requestFuture) {
            throw new RemotingException(String.format("request id(%d) duplicate!!!", requestId));
        }

        return current;
    }

    /**
     * 根据{@code requestId}获取request future
     * @param requestId request id
     * @return  request future
     */
    @Nullable
    protected CompletableFuture<Object> removeRequestFuture(long requestId){
        return requestFutureMap.remove(requestId);
    }
}
