package org.kin.kinrpc.message;

import org.kin.framework.concurrent.Dispatcher;
import org.kin.framework.concurrent.EventBasedDispatcher;
import org.kin.framework.concurrent.ExecutionContext;
import org.kin.framework.utils.ExtensionLoader;
import org.kin.framework.utils.NetUtils;
import org.kin.framework.utils.SysUtils;
import org.kin.kinrpc.config.ProtocolType;
import org.kin.kinrpc.config.SerializationType;
import org.kin.kinrpc.config.SslConfig;
import org.kin.kinrpc.transport.RemotingServer;
import org.kin.kinrpc.transport.RequestContext;
import org.kin.kinrpc.transport.RequestProcessor;
import org.kin.kinrpc.transport.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RPC环境
 * 管理一个server和多个client
 *
 * @author huangjianqin
 * @date 2020-06-08
 */
public final class ActorEnv {
    private static final Logger log = LoggerFactory.getLogger(ActorEnv.class);
    /** actor message processor interest */
    String ACTOR_INTEREST = "$actor";

    /** thread local {@link ActorEnv}实例 */
    private static final ThreadLocal<ActorEnv> THREAD_LOCAL_ACTOR_ENV = new ThreadLocal<>();

    static ActorEnv current() {
        return THREAD_LOCAL_ACTOR_ENV.get();
    }

    static void update(ActorEnv actorEnv) {
        THREAD_LOCAL_ACTOR_ENV.set(actorEnv);
    }
    //----------------------------------------------------------------------------------------------------------------
    /** 公用线程池, 除了dispatcher以外, 都用这个线程池, 会存在io/加锁操作 */
    ExecutionContext commonExecutors = ExecutionContext.elastic(SysUtils.getSuitableThreadNum(), SysUtils.CPU_NUM * 10,
            "kinrpc-message", 2);

    /** 消息调度 */
    private final Dispatcher<String, MessagePostContext> dispatcher;
    /** listen address */
    private final Address listenAddress;
    /** remoting server */
    private RemotingServer server;
    /** 传输层协议 */
    private final String protocol;
    /** 序列化方式 */
    private final String serialization;
    /** server端ssl */
    private SslConfig serverSslConfig;
    /** client端ssl */
    private SslConfig clientSsl;
    /** 标识是否terminated */
    private volatile boolean terminated = false;
    /** outbound client pool */
    private final Map<Address, MessageClient> clients = new ConcurrentHashMap<>();
    /** outBoxes */
    private final Map<Address, OutBox> outBoxes = new ConcurrentHashMap<>();
    /** key -> {@link Actor}实例, value -> {@link Actor}对应的{@link ActorRef}实例 */
    private final Map<Actor, ActorRef> actorRefMap = new ConcurrentHashMap<>();

    public ActorEnv(int port) {
        this(port, SysUtils.DOUBLE_CPU);
    }

    public ActorEnv(int port,
                    int parallelism) {
        this(NetUtils.getLocalhost4Ip(), port, parallelism, SerializationType.JSON, ProtocolType.KINRPC);
    }

    public ActorEnv(int port,
                    int parallelism,
                    SerializationType serializationType,
                    ProtocolType protocolType) {
        this(NetUtils.getLocalhost4Ip(), port, parallelism, serializationType, protocolType);
    }

    public ActorEnv(String host,
                    int port,
                    int parallelism,
                    SerializationType serializationType,
                    ProtocolType protocolType) {
        if (ProtocolType.JVM.equals(protocolType)) {
            throw new UnsupportedOperationException();
        }

        this.listenAddress = Address.of(host, port);
        this.dispatcher = new EventBasedDispatcher<>(parallelism);
        this.serialization = serializationType.getName();
        this.protocol = protocolType.getName();

        startServer();

//        //server child channel options, 默认值
//        Map<ChannelOption, Object> serverChannelOptions = new HashMap<>(6);
//        serverChannelOptions.put(ChannelOption.TCP_NODELAY, true);
//        serverChannelOptions.put(ChannelOption.SO_KEEPALIVE, true);
//        serverChannelOptions.put(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
//        //复用端口
//        serverChannelOptions.put(ChannelOption.SO_REUSEADDR, true);
//        //receive窗口缓存8mb
//        serverChannelOptions.put(ChannelOption.SO_RCVBUF, 8 * 1024 * 1024);
//        //send窗口缓存64kb
//        serverChannelOptions.put(ChannelOption.SO_SNDBUF, 64 * 1024);
//        updateServerChannelOptions(serverChannelOptions);
//
//        //client channel options, 默认值
//        Map<ChannelOption, Object> clientChannelOptions = new HashMap<>(6);
//        clientChannelOptions.put(ChannelOption.TCP_NODELAY, true);
//        clientChannelOptions.put(ChannelOption.SO_KEEPALIVE, true);
//        clientChannelOptions.put(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
//        clientChannelOptions.put(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
//        //receive窗口缓存8mb
//        clientChannelOptions.put(ChannelOption.SO_RCVBUF, 8 * 1024 * 1024);
//        //send窗口缓存64kb
//        clientChannelOptions.put(ChannelOption.SO_SNDBUF, 64 * 1024);
//        updateClientChannelOptions(clientChannelOptions);
    }

    /**
     * 检查是否已经terminated
     */
    private void checkTerminated() {
        if (!isTerminated()) {
            return;
        }

        throw new IllegalStateException("actor env is already terminated");
    }

    /**
     * start server
     */
    private void startServer() {
        checkTerminated();

        Transport transport = ExtensionLoader.getExtension(Transport.class, protocol);
        server = transport.createServer(listenAddress.getHost(), listenAddress.getPort(), null, serverSslConfig);
        server.registerRequestProcessor(new RequestProcessor<Serializable>() {
            @Override
            public void process(RequestContext requestContext, Serializable request) {

            }

            @Override
            public String interest() {
                return null;
            }
        });
        server.registerRequestProcessor(new MessagePayloadProcessor());
        server.start();
    }

    /**
     * 注册actor
     *
     * @param name  actor name
     * @param actor actor instance
     */
    public void newActor(String name, Actor actor) {
        checkTerminated();

        if (dispatcher.isRegistered(name)) {
            throw new IllegalStateException(String.format("actor '%s' has been registered", name));
        }

        ActorRef endpointRef = ActorRef.of(ActorAddress.of(listenAddress, name), this);
        actorRefMap.put(actor, endpointRef);
        dispatcher.register(name, actor, !actor.threadSafe());
    }

    /**
     * 注销actor
     */
    public void removeActor(String name, Actor actor) {
        checkTerminated();

        dispatcher.unregister(name);
        actorRefMap.remove(actor);
    }

    /**
     * create remote actor reference
     */
    public ActorRef actorOf(String host, int port, String actorName) {
        checkTerminated();

        return ActorRef.of(
                ActorAddress.of(
                        Address.of(host, port), actorName),
                this);
    }

    /**
     * create local actor reference
     */
    public ActorRef actorOf(String actorName) {
        return actorOf(listenAddress.getHost(), listenAddress.getPort(), actorName);
    }

    /**
     * destroy actor env
     */
    public void destroy() {
        if (isTerminated()) {
            return;
        }

        terminated = true;
        //移除outbox 及 client
        for (Address rpcAddress : outBoxes.keySet()) {
            removeOutBox(rpcAddress);
        }

        for (Address rpcAddress : clients.keySet()) {
            removeClient(rpcAddress);
        }

        //关闭注册endpoint
        dispatcher.shutdown();

        //关闭server
        server.shutdown();

        //shutdown 线程资源
        commonExecutors.shutdown();
    }

    //setter && getter
    public Address getListenAddress() {
        return listenAddress;
    }

    public String getSerialization() {
        return serialization;
    }

    public String getProtocol() {
        return protocol;
    }

    public SslConfig getServerSslConfig() {
        return serverSslConfig;
    }

    public SslConfig getClientSsl() {
        return clientSsl;
    }

    public boolean isTerminated() {
        return terminated;
    }

    //----------------------------------------------------------------------internal

    /**
     * push message to outbox
     */
    private void post2OutBox(OutBoxMessage message) {
        MessagePayload payload = message.getPayload();

        Address toAddress = payload.getToAddress();
        OutBox targetOutBox = outBoxes.computeIfAbsent(toAddress, k -> new OutBox(k, this));
        if (terminated) {
            removeOutBox(toAddress);
        } else {
            targetOutBox.pushMessage(message);
        }
    }

    /**
     * 发送消息
     */
    void fireAndForget(MessagePayload payload) {
        checkTerminated();

        if (listenAddress.equals(payload.getToAddress())) {
            //local, 直接分派
            postMessage(payload);
        } else {
            post2OutBox(new OutBoxMessage(payload));
        }
    }

    /**
     * 支持返回值为future的消息发送
     */
    <R extends Serializable> CompletableFuture<R> requestResponse(MessagePayload payload) {
        return requestResponse(payload, 0);
    }

    /**
     * 支持返回值为future的消息发送, 并且支持超时
     */
    <R extends Serializable> CompletableFuture<R> requestResponse(MessagePayload payload, long timeoutMs) {
        checkTerminated();
        //暴露给user的future
        CompletableFuture<R> userFuture = new CompletableFuture<>();
        post2OutBox(new OutBoxMessage(payload, userFuture, timeoutMs));
        return userFuture;
    }

    /**
     * 支持callback的消息发送
     */
    void requestResponse(MessagePayload messagePayload, MessageCallback callback) {
        requestResponse(messagePayload, callback, 0);
    }

    /**
     * 支持callback的消息发送, 并且支持超时
     */
    void requestResponse(MessagePayload messagePayload, MessageCallback callback, long timeoutMs) {
        checkTerminated();
        post2OutBox(new OutBoxMessage(messagePayload, callback, timeoutMs));
    }

    /**
     * 分派并处理接受到的消息
     */
    void postMessage(MessagePayload payload) {
        if (terminated) {
            return;
        }
        postMessage(null, payload);
    }

    /** 分派并处理消息 */
    private void postMessage(@Nullable RequestContext requestContext, MessagePayload payload) {
        MessagePostContext messagePostContext =
                new MessagePostContext(this, requestContext, payload.getFromAddress(), payload.getMessage());
        messagePostContext.setEventTime(System.currentTimeMillis());
        dispatcher.postMessage(payload.getActorName(), messagePostContext);
    }

    /**
     * 返回client
     *
     * @param address remote地址
     */
    MessageClient getClient(Address address) {
        checkTerminated();

        MessageClient client = clients.computeIfAbsent(address, k -> new MessageClient(this, address));
        if (!client.isConnect()) {
            commonExecutors.execute(client::connect);
        }

        return client;
    }

    /**
     * remove remote client
     */
    void removeClient(Address address) {
        MessageClient client = clients.remove(address);
        if (Objects.nonNull(client)) {
            client.shutdown();
        }
    }

    /**
     * remove outbox
     */
    void removeOutBox(Address address) {
        OutBox outBox = outBoxes.remove(address);
        if (Objects.nonNull(outBox)) {
            outBox.destroy();
        }
        removeClient(address);
    }

    /**
     * 返回actor reference
     */
    ActorRef actorOf(Actor actor) {
        return actorRefMap.get(actor);
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * 传输层对{@link MessagePayload}实例的处理
     */
    private class MessagePayloadProcessor implements RequestProcessor<MessagePayload> {
        @Override
        public void process(RequestContext requestContext, MessagePayload payload) {
            //分派
            postMessage(requestContext, payload);
        }

        @Override
        public String interest() {
            return ACTOR_INTEREST;
        }
    }
}
