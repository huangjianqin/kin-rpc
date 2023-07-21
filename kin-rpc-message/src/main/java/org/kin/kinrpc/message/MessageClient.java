package org.kin.kinrpc.message;

import org.kin.framework.collection.ConcurrentHashSet;
import org.kin.framework.concurrent.SimpleThreadFactory;
import org.kin.framework.concurrent.ThreadPoolUtils;
import org.kin.framework.utils.ExtensionException;
import org.kin.framework.utils.ExtensionLoader;
import org.kin.framework.utils.SysUtils;
import org.kin.kinrpc.transport.RemotingClient;
import org.kin.kinrpc.transport.RemotingClientStateObserver;
import org.kin.kinrpc.transport.Transport;
import org.kin.kinrpc.transport.TransportException;
import org.kin.kinrpc.transport.cmd.MessageCommand;
import org.kin.serialization.Serialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author huangjianqin
 * @date 2020-06-10
 */
final class MessageClient {
    private static final Logger log = LoggerFactory.getLogger(MessageClient.class);
    /** send message timeout scheduler */
    private static final ScheduledExecutorService SCHEDULER = ThreadPoolUtils.newScheduledThreadPool(
            "kinrpc-message-timeout-scheduler", true, SysUtils.CPU_NUM,
            new SimpleThreadFactory("kinrpc-message-timeout-scheduler", true),
            new ThreadPoolExecutor.CallerRunsPolicy());
    /** 初始状态 */
    private static final byte INIT_STATE = 1;
    /** exported状态 */
    private static final byte CONNECT_STATE = 2;
    /** unExported后状态 */
    private static final byte TERMINATED_STATE = 3;

    /** actor env */
    private final ActorEnv actorEnv;
    /** remote address */
    private final Address remoteAddress;
    /** client name */
    private final String name;
    /** serialization code */
    private final byte serializationCode;
    /** remoting client */
    private final RemotingClient client;
    /** 目前用于重连成功时, 触发OutBox消息重发逻辑 */
    private RemotingClientStateObserver clientStateObserver;
    /** send message futures */
    private final Set<CompletableFuture<?>> requestFutures = new ConcurrentHashSet<>();
    /** state */
    private AtomicInteger state = new AtomicInteger(INIT_STATE);

    MessageClient(RemotingActorEnv actorEnv,
                  Address remoteAddress) {
        this.actorEnv = actorEnv;
        this.remoteAddress = remoteAddress;
        this.name = getClass().getSimpleName() + String.format("(- R:%s)", remoteAddress);
        this.serializationCode = (byte) ExtensionLoader.getExtensionCode(Serialization.class, actorEnv.getSerialization());
        Transport transport = ExtensionLoader.getExtension(Transport.class, actorEnv.getProtocol());
        if (Objects.isNull(transport)) {
            throw new ExtensionException(String.format("can not find transport named '%s', please check whether related SPI config is missing", actorEnv.getProtocol()));
        }

        this.client = transport.createClient(remoteAddress.getHost(), remoteAddress.getPort(), actorEnv.getClientSslConfig());
    }

    /**
     * 连接remote
     */
    void connect() {
        if (!state.compareAndSet(INIT_STATE, CONNECT_STATE)) {
            return;
        }

        client.connect();
    }

    /**
     * 返回client是否可用
     *
     * @return true表示client可用
     */
    boolean isActive() {
        return isTerminated() && client.isAvailable();
    }

    /**
     * return client is terminated or not
     *
     * @return true表示client has been terminated
     */
    boolean isTerminated() {
        return state.get() != TERMINATED_STATE;
    }

    /**
     * return client connect or not
     *
     * @return true表示client has connect
     */
    boolean isConnect() {
        return state.get() >= CONNECT_STATE;
    }

    /**
     * shutdown client
     */
    void shutdown() {
        if (!state.compareAndSet(CONNECT_STATE, TERMINATED_STATE)) {
            return;
        }

        client.shutdown();

        for (CompletableFuture<?> requestFuture : requestFutures) {
            requestFuture.completeExceptionally(new TransportException(String.format("%s terminated", name)));
        }
    }

    /**
     * send message
     *
     * @param outBoxMessage outbound message
     */
    void send(OutBoxMessage outBoxMessage) {
        if (!isActive()) {
            return;
        }

        MessagePayload payload = outBoxMessage.getPayload();
        //设置的超时时间, 这里更新为超时结束时间
        if (payload.getTimeout() > 0) {
            payload.setTimeout(System.currentTimeMillis() + payload.getTimeout());
        }
        MessageCommand command = new MessageCommand(serializationCode, payload);

        //request
        CompletableFuture<?> requestFuture = payload.isIgnoreResponse() ? client.fireAndForget(command) : client.requestResponse(command);
        requestFutures.add(requestFuture);

        //schedule timeout
        ScheduledFuture<?> timeoutFuture = null;
        long timeoutMs = outBoxMessage.getTimeoutMs();
        if (timeoutMs > 0) {
            timeoutFuture = SCHEDULER.schedule(() ->
                            requestFuture.completeExceptionally(new MessageTimeoutException(outBoxMessage.getPayload().getMessage()))
                    , timeoutMs, TimeUnit.MILLISECONDS);
        }

        //pre handle response
        ScheduledFuture<?> finalTimeoutFuture = timeoutFuture;
        requestFuture.whenCompleteAsync((r, t) -> {
            if (Objects.nonNull(finalTimeoutFuture) && !finalTimeoutFuture.isDone()) {
                finalTimeoutFuture.cancel(true);
            }

            requestFutures.remove(requestFuture);
            outBoxMessage.complete((MessagePayload) r, t);
        }, actorEnv.commonExecutors);
    }

    //setter && getter
    void setClientStateObserver(RemotingClientStateObserver clientStateObserver) {
        this.client.addObservers(clientStateObserver);
        this.clientStateObserver = clientStateObserver;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageClient that = (MessageClient) o;
        return Objects.equals(remoteAddress, that.remoteAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(remoteAddress);
    }
}
