package org.kin.kinrpc.message;

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
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 扩展{@link ActorEnv}, 支持remoting
 *
 * @author huangjianqin
 * @date 2023/7/14
 */
public final class RemotingActorEnv extends ActorEnv {
    private static final Logger log = LoggerFactory.getLogger(RemotingActorEnv.class);

    /** listen address */
    private final Address listenAddress;
    /** remoting server */
    private RemotingServer server;
    /** 传输层协议 */
    private final String protocol;
    /** 序列化方式 */
    private final String serialization;
    /** server端ssl */
    private final SslConfig serverSslConfig;
    /** client端ssl */
    private final SslConfig clientSslConfig;
    /** 标识是否terminated */
    private volatile boolean terminated = false;
    /** outbound client pool */
    private final Map<Address, MessageClient> clients = new ConcurrentHashMap<>();
    /** outBoxes */
    private final Map<Address, OutBox> outBoxes = new ConcurrentHashMap<>();

    private RemotingActorEnv(String host,
                             int port,
                             int parallelism,
                             SerializationType serializationType,
                             ProtocolType protocolType,
                             SslConfig serverSslConfig,
                             SslConfig clientSslConfig) {
        super(parallelism, new ActorRefProvider<RemotingActorEnv>() {
            @Override
            public ActorRef actorOf(RemotingActorEnv actorEnv, ActorPath actorPath) {
                Address address = actorPath.getAddress();
                if (Objects.isNull(address)) {
                    address = Address.LOCAL;
                }
                if (address.isLocal() || address.equals(actorEnv.getListenAddress())) {
                    return new LocalActorRef(actorPath, actorEnv);
                } else {
                    return new RemotingActorRef(actorPath, actorEnv);
                }
            }

            @Override
            public ActorRef actorOf(RemotingActorEnv actorEnv, String actorName) {
                return new RemotingActorRef(ActorPath.of(actorEnv.getListenAddress(), actorName), actorEnv);
            }
        });

        if (ProtocolType.JVM.equals(protocolType)) {
            throw new UnsupportedOperationException();
        }

        this.listenAddress = Address.of(host, port);
        this.serialization = serializationType.getName();
        this.protocol = protocolType.getName();
        this.serverSslConfig = serverSslConfig;
        this.clientSslConfig = clientSslConfig;

        startServer();

        // TODO: 2023/7/14
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
     * start server
     */
    private void startServer() {
        checkTerminated();

        Transport transport = ExtensionLoader.getExtension(Transport.class, protocol);
        server = transport.createServer(listenAddress.getHost(), listenAddress.getPort(), null, serverSslConfig);
        server.registerRequestProcessor(new MessagePayloadProcessor());
        server.start();
    }

    @Override
    protected void doDestroy() {
        super.doDestroy();
        //移除outbox 及 client
        for (Address address : outBoxes.keySet()) {
            removeOutBox(address);
        }

        for (Address address : clients.keySet()) {
            removeClient(address);
        }

        //关闭server
        server.shutdown();
    }

    //setter && getter
    Address getListenAddress() {
        return listenAddress;
    }

    String getSerialization() {
        return serialization;
    }

    String getProtocol() {
        return protocol;
    }

    SslConfig getServerSslConfig() {
        return serverSslConfig;
    }

    SslConfig getClientSslConfig() {
        return clientSslConfig;
    }

    //----------------------------------------------------------------------internal

    /**
     * push message to outbox
     *
     * @param message message which waiting to send
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
     *
     * @param payload message payload
     */
    void tell(MessagePayload payload) {
        checkTerminated();

        post2OutBox(new OutBoxMessage(payload));
    }

    /**
     * 支持返回值为future的消息发送
     *
     * @param payload message payload
     */
    CompletableFuture<Object> ask(MessagePayload payload) {
        return ask(payload, 0);
    }

    /**
     * 支持返回值为future的消息发送, 并且支持超时
     *
     * @param payload   message payload
     * @param timeoutMs send message and receive response message timeout
     */
    CompletableFuture<Object> ask(MessagePayload payload, long timeoutMs) {
        checkTerminated();
        //暴露给user的future
        CompletableFuture<Object> userFuture = new CompletableFuture<>();
        post2OutBox(new OutBoxMessage(payload, userFuture, timeoutMs));
        return userFuture;
    }

    /**
     * 分派并处理接受到的消息
     *
     * @param payload message payload
     */
    void postMessage(MessagePayload payload) {
        postMessage(null, payload);
    }

    /**
     * 分派并处理消息
     *
     * @param requestContext transport request context
     * @param payload        message payload
     */
    private void postMessage(@Nullable RequestContext requestContext, MessagePayload payload) {
        if (terminated) {
            return;
        }

        ActorRef sender;
        ActorPath fromActorAddress = payload.getFromActorAddress();


        if (Objects.isNull(requestContext)) {
            sender = new LocalActorRef(fromActorAddress, this);
        } else {
            sender = new RemotingActorRef(fromActorAddress, RemotingActorEnv.this,
                    requestContext, payload.getToActorName());
        }
        ActorContext actorContext =
                new ActorContext(RemotingActorEnv.this, sender,
                        ActorPath.of(payload.getToActorName()), payload.getMessage());
        RemotingActorEnv.this.postMessage(actorContext);
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
     *
     * @param address remote地址
     */
    void removeClient(Address address) {
        MessageClient client = clients.remove(address);
        if (Objects.nonNull(client)) {
            client.shutdown();
        }
    }

    /**
     * remove outbox
     *
     * @param address remote地址
     */
    void removeOutBox(Address address) {
        OutBox outBox = outBoxes.remove(address);
        if (Objects.nonNull(outBox)) {
            outBox.destroy();
        }
        removeClient(address);
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * 传输层对{@link MessagePayload}实例的处理
     */
    private class MessagePayloadProcessor implements RequestProcessor<MessagePayload> {
        @Override
        public void process(RequestContext requestContext, MessagePayload payload) {
            long timeout = payload.getTimeout();
            if (timeout > 0 && System.currentTimeMillis() > timeout) {
                //message timeout, ignore
                return;
            }
            //分派
            postMessage(requestContext, payload);
        }

        @Override
        public String interest() {
            return MessagePayload.class.getName();
        }
    }

    public static class Builder {
        /** listen host */
        private String host = NetUtils.getLocalhost4Ip();
        /** listen port */
        private int port = 12888;
        /** 并行数 */
        private int parallelism = SysUtils.DOUBLE_CPU;
        /** 传输层协议 */
        private ProtocolType protocolType = ProtocolType.KINRPC;
        /** 序列化方式 */
        private SerializationType serializationType = SerializationType.JSON;
        /** server端ssl */
        private SslConfig serverSslConfig;
        /** client端ssl */
        private SslConfig clientSslConfig;

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder parallelism(int parallelism) {
            this.parallelism = parallelism;
            return this;
        }

        public Builder protocolType(ProtocolType protocolType) {
            this.protocolType = protocolType;
            return this;
        }

        public Builder serializationType(SerializationType serializationType) {
            this.serializationType = serializationType;
            return this;
        }

        public Builder serverSslConfig(SslConfig serverSslConfig) {
            this.serverSslConfig = serverSslConfig;
            return this;
        }

        public Builder clientSslConfig(SslConfig clientSslConfig) {
            this.clientSslConfig = clientSslConfig;
            return this;
        }

        public ActorEnv build() {
            return new RemotingActorEnv(host, port,
                    parallelism, serializationType,
                    protocolType, serverSslConfig, clientSslConfig);
        }
    }
}
