package org.kin.kinrpc.transport;

import org.jctools.maps.NonBlockingHashMap;
import org.kin.framework.concurrent.SimpleThreadFactory;
import org.kin.framework.concurrent.ThreadPoolUtils;
import org.kin.framework.utils.SysUtils;
import org.kin.kinrpc.executor.DefaultManagedExecutor;
import org.kin.kinrpc.executor.ManagedExecutor;
import org.kin.kinrpc.transport.cmd.RemotingCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * {@link RemotingClient}抽象, 支持心跳健康检查, 自动重连
 *
 * @author huangjianqin
 * @date 2023/6/7
 */
public abstract class AbstractRemotingClient implements RemotingClient {
    private static final Logger log = LoggerFactory.getLogger(AbstractRemotingClient.class);

    /** remoting codec */
    protected final RemotingCodec codec = new RemotingCodec();
    /** remoting processor */
    protected final RemotingProcessor remotingProcessor;
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
            return AbstractRemotingClient.this.removeRequestFuture(requestId);
        }
    };
    /** client是否可用 */
    protected volatile boolean available;
    /** reconnect signal */
    private volatile CompletableFuture<Void> reconnectSignal;
    /** remoting client helper */
    private final RemotingClientHelper helper = new RemotingClientHelper() {
        @Override
        public CompletableFuture<Void> heartbeat() {
            return AbstractRemotingClient.this.heartbeat();
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
            synchronized (AbstractRemotingClient.this) {
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
                AbstractRemotingClient.this.onReconnect();
                return reconnectSignal;
            }
        }

        @Override
        public String getName() {
            return AbstractRemotingClient.this.name;
        }

        @Override
        public boolean isTerminated() {
            return terminated;
        }
    };
    /** 是否terminated */
    private volatile boolean terminated;
    /** client name */
    private final String name;
    /** 用于阻塞等待首次连接完成 */
    private volatile CountDownLatch firstConnWaiter = new CountDownLatch(1);
    /** 已注册的remoting client state observer */
    private final List<RemotingClientStateObserver> observers = new CopyOnWriteArrayList<>();

    protected AbstractRemotingClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.remoteAddress = new InetSocketAddress(host, port);
        this.name = getClass().getSimpleName() + String.format("(- R:%s)", remoteAddress());
        String executorName = name + "-command-processor";
        ManagedExecutor executor = new DefaultManagedExecutor(
                ThreadPoolUtils.newThreadPool(executorName, true,
                        SysUtils.CPU_NUM, SysUtils.DOUBLE_CPU,
                        60, TimeUnit.SECONDS,
                        new LinkedBlockingQueue<>(256), new SimpleThreadFactory(executorName),
                        new ThreadPoolExecutor.CallerRunsPolicy()));
        this.remotingProcessor = new RemotingProcessor(codec, executor);
    }

    @Override
    public final void connect() {
        checkTerminated();
        onConnect();

        //等待连接完成
        if (Objects.nonNull(firstConnWaiter)) {
            try {
                firstConnWaiter.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
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
    protected final void onConnectSuccess() {
        if (isTerminated()) {
            return;
        }

        try {
            synchronized (this) {
                if (!isReconnecting()) {
                    //connect success
                    log.info("{} connect to {} success", name(), remoteAddress());
                    RemotingClientHealthManager.addClient(helper);
                } else {
                    //reconnect success
                    log.info("{} reconnect to {} success", name(), remoteAddress());
                    onReconnectSuccess();
                }
            }

            fireStateObserver(o -> o.onConnectSuccess(this));
        } finally {
            if (Objects.nonNull(firstConnWaiter)) {
                firstConnWaiter.countDown();
                firstConnWaiter = null;
            }
        }
    }

    /**
     * connect fail
     */
    protected final void onConnectFail(Throwable t) {
        if (isTerminated()) {
            return;
        }

        try {
            synchronized (this) {
                if (!isReconnecting()) {
                    log.error("{} connect to {} fail", name(), remoteAddress(), t);
                    available = false;
                    RemotingClientHealthManager.onConnectFail(helper, t);
                } else {
                    //重连中, 交给monitor继续重试
                    onReconnectFail(t);
                }
            }
        } finally {
            if (Objects.nonNull(firstConnWaiter)) {
                firstConnWaiter.countDown();
                firstConnWaiter = null;
            }
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
        RemotingClientHealthManager.onRequestFail(helper, t);
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

        fireStateObserver(o -> o.onReconnectSuccess(this));
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
        RemotingClientHealthManager.onClientTerminated(helper);

        fireStateObserver(o -> o.onTerminated(this));
    }

    @Override
    public final void shutdown() {
        if (isTerminated()) {
            return;
        }

        terminated = true;
        onShutdown();

        for (CompletableFuture<Object> future : requestFutureMap.values()) {
            future.completeExceptionally(new TransportException(String.format("%s has terminated", name())));
        }

        RemotingClientHealthManager.removeClient(helper);
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

    @Override
    public void addObservers(Collection<RemotingClientStateObserver> observers) {
        this.observers.addAll(observers);
    }

    /**
     * 触发client state observer响应action
     */
    private void fireStateObserver(Consumer<RemotingClientStateObserver> observerAction) {
        for (RemotingClientStateObserver observer : observers) {
            try {
                observerAction.accept(observer);
            } catch (Exception e) {
                log.error("fire client state observer error", e);
            }
        }
    }

    //getter
    @Override
    public final String remoteAddress() {
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
