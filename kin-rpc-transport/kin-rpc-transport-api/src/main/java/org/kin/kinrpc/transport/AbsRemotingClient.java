package org.kin.kinrpc.transport;

import org.jctools.maps.NonBlockingHashMap;
import org.kin.kinrpc.transport.cmd.RemotingCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * @author huangjianqin
 * @date 2023/6/7
 */
public abstract class AbsRemotingClient implements RemotingClient {
    private static final Logger log = LoggerFactory.getLogger(AbsRemotingClient.class);

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
    /** client是否可用 */
    protected volatile boolean available;
    /** reconnect signal */
    private volatile CompletableFuture<Void> reconnectSignal;
    /** remoting client observer */
    private final RemotingClientObserver observer = new RemotingClientObserver() {
        @Override
        public CompletableFuture<Void> heartbeat() {
            return AbsRemotingClient.this.heartbeat();
        }

        @Override
        public void toUnhealth() {
            available = false;
        }

        @Override
        public void toHealth() {
            available = true;
        }

        @Nullable
        @Override
        public CompletableFuture<Void> reconnect() {
            synchronized (AbsRemotingClient.this) {
                if (isTerminated()) {
                    return null;
                }

                if (isAvailable()) {
                    return null;
                }

                if (isReconnecting()) {
                    return null;
                }

                log.warn("{} start to reconnect", getName());
                reconnectSignal = new CompletableFuture<>();
                AbsRemotingClient.this.onReconnect();
                return reconnectSignal;
            }
        }

        @Override
        public String getName() {
            return AbsRemotingClient.this.name;
        }
    };
    /** 是否terminated */
    private volatile boolean terminated;
    /** client name */
    private final String name;

    protected AbsRemotingClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.remoteAddress = new InetSocketAddress(host, port);
        this.name = getClass().getSimpleName() + String.format("(- R:%s)", remoteAddress());
    }

    @Override
    public final void connect() {
        checkTerminated();
        onConnect();
    }

    /**
     * remote connect
     */
    protected abstract void onConnect();

    /**
     * 重连
     */
    protected abstract void onReconnect();

    /**
     * connect success
     */
    protected final synchronized void onConnectSuccess() {
        if (isTerminated()) {
            return;
        }

        if (!isReconnecting()) {
            //connect success
            log.info("{} connect to {} success", name(), remoteAddress());
            RemotingClientMonitor.addClient(observer);
        } else {
            //reconnect success
            log.info("{} reconnect to {} success", name(), remoteAddress());
            onReconnectSuccess();
        }
    }

    /**
     * connect fail
     */
    protected final synchronized void onConnectFail(Throwable t) {
        if (isTerminated()) {
            return;
        }

        if (!isReconnecting()) {
            log.error("{} connect to {} fail", name(), remoteAddress(), t);
            available = false;
            RemotingClientMonitor.onConnectFail(observer, t);
        } else {
            //重连中, 交给monitor继续重试
            onReconnectFail(t);
        }
    }

    /**
     * request fail
     */
    protected synchronized void onRequestFail(Throwable t) {
        if (isTerminated()) {
            return;
        }

        if (isReconnecting()) {
            //重连中, 忽略
            return;
        }

        available = false;
        RemotingClientMonitor.onRequestFail(observer, t);
    }

    /**
     * reconnect success
     */
    private synchronized void onReconnectSuccess() {
        if (isTerminated()) {
            return;
        }
        CompletableFuture<Void> reconnectSignal = this.reconnectSignal;
        if (Objects.isNull(reconnectSignal)) {
            return;
        }

        this.reconnectSignal = null;
        reconnectSignal.complete(null);
    }

    /**
     * reconnect fail
     */
    private synchronized void onReconnectFail(Throwable t) {
        if (isTerminated()) {
            return;
        }
        CompletableFuture<Void> reconnectSignal = this.reconnectSignal;
        if (Objects.isNull(reconnectSignal)) {
            return;
        }

        this.reconnectSignal = null;
        reconnectSignal.completeExceptionally(t);
    }

    /**
     * client connection closed
     */
    protected void onConnectionClosed() {
        if (isTerminated()) {
            //after shutdown
            return;
        }

        log.info("{} connection closed", name());

        //remote down or remote force close connection
        RemotingClientMonitor.onClientTerminated(observer);
    }

    @Override
    public final void shutdown() {
        if (isTerminated()) {
            return;
        }

        terminated = true;
        onShutdown();

        RemotingClientMonitor.removeClient(observer);
    }

    /**
     * shutdown the client
     */
    protected abstract void onShutdown();

    /**
     * 根据{@code requestId}创建request future
     *
     * @return request future
     */
    protected final CompletableFuture<Object> createRequestFuture(long requestId) {
        CompletableFuture<Object> requestFuture = new CompletableFuture<>();
        CompletableFuture<Object> current = requestFutureMap.computeIfAbsent(requestId, i -> requestFuture);
        if (current != requestFuture) {
            throw new RemotingException(String.format("request id(%d) duplicate!!!", requestId));
        }

        return current;
    }

    /**
     * 根据{@code requestId}获取request future
     *
     * @param requestId request id
     * @return request future
     */
    @Nullable
    protected final CompletableFuture<Object> removeRequestFuture(long requestId) {
        return requestFutureMap.remove(requestId);
    }

    /**
     * 发送心跳
     *
     * @return 心跳response signal
     */
    protected abstract CompletableFuture<Void> heartbeat();

    /**
     * check remoting client is available or not
     */
    protected void checkAvailable() {
        if (!isAvailable()) {
            throw new IllegalStateException(String.format("%s is not available", name()));
        }
    }

    /**
     * check remoting client is terminated or not
     */
    protected void checkTerminated() {
        if (isTerminated()) {
            throw new IllegalStateException(String.format("%s is terminated", name()));
        }
    }

    /**
     * client是否重连中
     *
     * @return true表示 client重连中
     */
    protected boolean isReconnecting() {
        return Objects.nonNull(reconnectSignal);
    }

    //getter

    /**
     * 返回remote address
     *
     * @return remote address
     */
    protected final String remoteAddress() {
        return host + ":" + port;
    }

    @Override
    public final boolean isAvailable() {
        return available && !isTerminated();
    }

    public boolean isTerminated() {
        return terminated;
    }

    public String name() {
        return name;
    }
}
