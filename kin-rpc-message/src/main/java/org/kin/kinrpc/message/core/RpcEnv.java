package org.kin.kinrpc.message.core;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import org.kin.framework.concurrent.ExecutionContext;
import org.kin.framework.concurrent.actor.Dispatcher;
import org.kin.framework.concurrent.actor.EventBasedDispatcher;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.framework.utils.SysUtils;
import org.kin.kinrpc.message.core.message.ClientConnected;
import org.kin.kinrpc.message.core.message.ClientDisconnected;
import org.kin.kinrpc.message.transport.TransportClient;
import org.kin.kinrpc.message.transport.domain.RpcEndpointAddress;
import org.kin.kinrpc.message.transport.protocol.RpcMessage;
import org.kin.kinrpc.rpc.common.SslConfig;
import org.kin.kinrpc.serializer.Serializer;
import org.kin.kinrpc.serializer.SerializerType;
import org.kin.kinrpc.serializer.Serializers;
import org.kin.kinrpc.serializer.UnknownSerializerException;
import org.kin.kinrpc.transport.kinrpc.KinRpcAddress;
import org.kin.kinrpc.transport.kinrpc.KinRpcEndpointHandler;
import org.kin.kinrpc.transport.kinrpc.KinRpcRequestProtocol;
import org.kin.transport.netty.CompressionType;
import org.kin.transport.netty.Transports;
import org.kin.transport.netty.socket.protocol.ProtocolFactory;
import org.kin.transport.netty.socket.protocol.ProtocolStatisicService;
import org.kin.transport.netty.socket.server.SocketServerTransportOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RPC环境
 * 管理一个server和多个client
 *
 * @author huangjianqin
 * @date 2020-06-08
 */
public final class RpcEnv {
    private static final Logger log = LoggerFactory.getLogger(RpcEnv.class);

    static {
        //加载底层通信协议
        ProtocolFactory.init(KinRpcRequestProtocol.class.getPackage().getName());
    }

    /** 线程本地RpcEnv */
    private static ThreadLocal<RpcEnv> currentRpcEnv = new ThreadLocal<>();

    public static RpcEnv currentRpcEnv() {
        return currentRpcEnv.get();
    }

    public static void updateCurrentRpcEnv(RpcEnv rpcEnv) {
        currentRpcEnv.set(rpcEnv);
    }
    //----------------------------------------------------------------------------------------------------------------
    /**
     * rpc环境公用线程池, 除了dispatcher以外, 都用这个线程池
     */
    public ExecutionContext commonExecutors = ExecutionContext.elastic(SysUtils.getSuitableThreadNum(), SysUtils.CPU_NUM * 10,
            "rpc-message", 2, "rpc-message-scheduler");

    /** 事件调度 */
    private Dispatcher<String, RpcMessageCallContext> dispatcher;
    private final KinRpcAddress address;
    /** 序列化方式 */
    private final Serializer serializer;
    /** 通讯层是否支持压缩 */
    private final CompressionType compressionType;
    /** 服务器 */
    private RpcEndpointImpl rpcEndpoint;
    /** 标识是否stopped */
    private volatile boolean isStopped = false;
    /** 基于本rpc环境下的client */
    private final Map<KinRpcAddress, TransportClient> clients = new ConcurrentHashMap<>();
    /** outBoxs */
    private Map<KinRpcAddress, OutBox> outBoxs = new ConcurrentHashMap<>();
    /** key -> RpcEndpoint, value -> RpcEndpoint对应的RpcEndpointRef */
    private Map<RpcEndpoint, RpcEndpointRef> endpoint2Ref = new ConcurrentHashMap<>();

    public RpcEnv(String host, int port) {
        this(host, port, SysUtils.getSuitableThreadNum(),
                Serializers.getSerializer(SerializerType.KRYO.getCode()), CompressionType.NONE);
    }

    public RpcEnv(String host, int port, int parallelism) {
        this(host, port, parallelism,
                Serializers.getSerializer(SerializerType.KRYO.getCode()), CompressionType.NONE);
    }

    public RpcEnv(String host, int port, Serializer serializer) {
        this(host, port, SysUtils.getSuitableThreadNum(),
                serializer, org.kin.transport.netty.CompressionType.NONE);
    }

    public RpcEnv(String host, int port, int parallelism, Serializer serializer) {
        this(host, port, parallelism,
                serializer, org.kin.transport.netty.CompressionType.NONE);
    }

    public RpcEnv(String host, int port, int parallelism, CompressionType compressionType) {
        this(host, port, parallelism,
                Serializers.getSerializer(SerializerType.KRYO.getCode()), compressionType);
    }

    public RpcEnv(String host, int port, int parallelism, Serializer serializer, CompressionType compressionType) {
        this.address = KinRpcAddress.of(host, port);
        this.dispatcher = new EventBasedDispatcher<>(parallelism);
        this.serializer = serializer;
        this.compressionType = compressionType;
    }

    //--------------------------------------------------------------------------------------------------------------------------------

    /**
     * 启动rpc环境, 即绑定某端口的服务器
     */
    public final void startServer() {
        if (isStopped) {
            return;
        }

        rpcEndpoint = new RpcEndpointImpl();

        String host = address.getHost();
        int port = address.getPort();

        InetSocketAddress address;
        if (StringUtils.isNotBlank(host)) {
            address = new InetSocketAddress(host, port);
        } else {
            address = new InetSocketAddress(port);
        }

        SocketServerTransportOption.SocketServerTransportOptionBuilder builder =
                Transports.socket().server()
                        .channelOption(ChannelOption.TCP_NODELAY, true)
                        .channelOption(ChannelOption.SO_KEEPALIVE, true)
                        .channelOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                        //复用端口
                        .channelOption(ChannelOption.SO_REUSEADDR, true)
                        //receive窗口缓存6mb
                        .channelOption(ChannelOption.SO_RCVBUF, 10 * 1024 * 1024)
                        //send窗口缓存64kb
                        .channelOption(ChannelOption.SO_SNDBUF, 64 * 1024)
                        .protocolHandler(rpcEndpoint)
                        .compress(compressionType);

        String certPath = SslConfig.INSTANCE.getServerKeyCertChainPath();
        String keyPath = SslConfig.INSTANCE.getServerPrivateKeyPath();

        if (StringUtils.isNotBlank(certPath) && StringUtils.isNotBlank(keyPath)) {
            builder.ssl(certPath, keyPath);
        }

        SocketServerTransportOption transportOption = builder.build();

        try {
            rpcEndpoint.bind(transportOption, address);
        } catch (Exception e) {
            ExceptionUtils.throwExt(e);
        }
    }

    /**
     * 注册rpc服务
     *
     * @param name        rpc服务名
     * @param rpcEndpoint rpc服务消息处理实现
     */
    public final void register(String name, RpcEndpoint rpcEndpoint) {
        if (isStopped) {
            return;
        }

        RpcEndpointRef endpointRef = RpcEndpointRef.of(RpcEndpointAddress.of(address, name), this);
        endpoint2Ref.put(rpcEndpoint, endpointRef);
        dispatcher.register(name, rpcEndpoint, !rpcEndpoint.threadSafe());
    }

    /**
     * 注销rpc服务
     */
    public final void unregister(String name, RpcEndpoint rpcEndpoint) {
        if (isStopped) {
            return;
        }

        dispatcher.unregister(name);
        endpoint2Ref.remove(rpcEndpoint);
    }

    /**
     * 关闭rpc环境, 即关闭绑定某端口的服务器
     */
    public final void stop() {
        if (isStopped) {
            return;
        }

        if (CollectionUtils.isNonEmpty(endpoint2Ref)) {
            return;
        }

        isStopped = true;
        //移除outbox 及 client
        for (KinRpcAddress rpcAddress : outBoxs.keySet()) {
            removeOutBox(rpcAddress);
        }

        for (KinRpcAddress rpcAddress : clients.keySet()) {
            removeClient(rpcAddress);
        }

        //关闭注册endpoint
        dispatcher.shutdown();

        //关闭server
        if (Objects.nonNull(rpcEndpoint)) {
            rpcEndpoint.close();
        }

        //shutdown 线程资源
        commonExecutors.shutdown();
    }

    /**
     * 反序列化消息
     *
     * @param serializer 目标序列化类型
     */
    public RpcMessage deserialize(Serializer serializer, byte[] data) {
        //更新线程本地RpcEnv
        updateCurrentRpcEnv(this);

        RpcMessage message;
        try {
            //反序列化
            message = serializer.deserialize(data, RpcMessage.class);
            //统计消息次数及长度
            ProtocolStatisicService.instance().statisticReq(
                    message.getMessage().getClass().getName(), data.length
            );
            return message;
        } catch (IOException | ClassNotFoundException e) {
            ExceptionUtils.throwExt(e);
        }
        return null;
    }

    /**
     * 序列化消息
     */
    public byte[] serialize(RpcMessage message) {
        try {
            //序列化
            byte[] data = serializer.serialize(message);
            //统计消息次数及长度
            ProtocolStatisicService.instance().statisticReq(
                    message.getMessage().getClass().getName(), data.length
            );

            return data;
        } catch (IOException e) {
            ExceptionUtils.throwExt(e);
        }
        return null;
    }

    /**
     * 发送消息
     */
    public void send(RpcMessage message) {
        if (isStopped) {
            return;
        }
        RpcEndpointAddress endpointAddress = message.getTo().getEndpointAddress();
        if (address.equals(endpointAddress.getRpcAddress())) {
            //local, 直接分派
            postMessage(message);
        } else {
            post2OutBox(new OutBoxMessage(message));
        }
    }

    /**
     * 把消息推到outbox
     */
    private void post2OutBox(OutBoxMessage message) {
        RpcEndpointAddress endpointAddress = message.getMessage().getTo().getEndpointAddress();

        OutBox targetOutBox;
        KinRpcAddress toRpcAddress = endpointAddress.getRpcAddress();
        targetOutBox = outBoxs.get(toRpcAddress);
        if (Objects.isNull(targetOutBox)) {
            OutBox newOutBox = new OutBox(toRpcAddress, this);
            OutBox oldOutBox = outBoxs.putIfAbsent(toRpcAddress, newOutBox);
            if (Objects.isNull(oldOutBox)) {
                targetOutBox = newOutBox;
            } else {
                targetOutBox = oldOutBox;
            }
        }

        if (isStopped) {
            removeOutBox(toRpcAddress);
        } else {
            targetOutBox.sendMessage(message);
        }
    }

    /**
     * 支持future的消息发送
     */
    public <R extends Serializable> RpcFuture<R> ask(RpcMessage message) {
        if (isStopped) {
            throw new IllegalStateException("rpcEnv stopped");
        }
        RpcFuture<R> future = new RpcFuture<>(this, message.getTo().getEndpointAddress().getRpcAddress(), message.getRequestId());
        RpcResponseCallback<R> callback = new RpcResponseCallback<R>() {
            @Override
            public void onSuccess(R message) {
                future.done(message);
            }

            @Override
            public void onFail(Throwable e) {
                future.fail(e);
            }
        };
        post2OutBox(new OutBoxMessage(message, callback));
        return future;
    }

    /**
     * 分派并处理接受到的消息
     */
    public void postMessage(RpcMessage message) {
        if (isStopped) {
            return;
        }
        postMessage(null, message);
    }

    /**
     * 分派并处理接受到的消息
     */
    public void postMessage(Channel channel, RpcMessage message) {
        RpcMessageCallContext rpcMessageCallContext =
                new RpcMessageCallContext(this, message.getFromAddress(), channel, message.getTo(), message.getMessage(), message.getRequestId(), message.getCreateTime());
        rpcMessageCallContext.setEventTime(System.currentTimeMillis());
        dispatcher.postMessage(message.getTo().getEndpointAddress().getName(), rpcMessageCallContext);
    }

    public RpcEndpointRef createEndpointRef(String host, int port, String receiverName) {
        if (isStopped) {
            throw new IllegalStateException("rpcEnv stopped");
        }
        return RpcEndpointRef.of(
                RpcEndpointAddress.of(
                        KinRpcAddress.of(host, port), receiverName),
                this);
    }

    /**
     * 获取客户端
     *
     * @param address remote地址
     */
    public TransportClient getClient(KinRpcAddress address) {
        if (isStopped) {
            throw new IllegalStateException("rpcEnv stopped");
        }

        //加锁处理, 保证获取一个active client
        TransportClient transportClient;
        synchronized (clients) {
            transportClient = clients.get(address);
            if (Objects.nonNull(transportClient)) {
                if (transportClient.isActive()) {
                    return transportClient;
                }
                transportClient.stop();
            }
            transportClient = new TransportClient(this, address, compressionType);
            transportClient.connect();
            clients.put(address, transportClient);
        }

        return transportClient;
    }

    /**
     * 移除启动了的客户端
     */
    public void removeClient(KinRpcAddress address) {
        synchronized (clients) {
            TransportClient client = clients.remove(address);
            if (Objects.nonNull(client)) {
                client.stop();
            }
        }
    }

    /**
     * 移除outbox
     */
    public void removeOutBox(KinRpcAddress address) {
        OutBox outBox = outBoxs.remove(address);
        if (Objects.nonNull(outBox)) {
            outBox.stop();
        }
        removeClient(address);
    }


    //------------------------------------------------------------------------------------------------------------------

    /**
     * 该rpc环境的地址
     */
    public KinRpcAddress address() {
        return address;
    }

    /**
     * 该rpc环境的rpcEndpointRef
     */
    public RpcEndpointRef rpcEndpointRef(RpcEndpoint endpoint) {
        return endpoint2Ref.get(endpoint);
    }

    /**
     * rpc环境公用线程池
     */
    public ExecutionContext executors() {
        return commonExecutors;
    }

    /**
     * dispatcher
     */
    public Dispatcher<String, RpcMessageCallContext> dispatcher() {
        return dispatcher;
    }

    /** serializer */
    public Serializer serializer() {
        return serializer;
    }

    //------------------------------------------------------------------------------------------------------------------
    private class RpcEndpointImpl extends KinRpcEndpointHandler {
        /** client连接信息 -> 远程服务绑定的端口信息 */
        private Map<KinRpcAddress, KinRpcAddress> clientAddr2RemoteBindAddr = new ConcurrentHashMap<>();

        @Override
        protected final void handleRpcRequestProtocol(Channel channel, KinRpcRequestProtocol requestProtocol) {
            byte serializerType = requestProtocol.getSerializer();
            //反序列化内容
            byte[] data = requestProtocol.getReqContent();

            Serializer serializer = Serializers.getSerializer(serializerType);
            if (Objects.isNull(serializer)) {
                throw new UnknownSerializerException(serializerType);
            }
            RpcMessage message = deserialize(serializer, data);
            if (Objects.isNull(message)) {
                return;
            }

            InetSocketAddress remoteAddress = (InetSocketAddress) channel.remoteAddress();
            KinRpcAddress clientAddr = KinRpcAddress.of(remoteAddress.getHostName(), remoteAddress.getPort());
            clientAddr2RemoteBindAddr.put(clientAddr, message.getFromAddress());

            //分派
            postMessage(channel, message);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            super.channelActive(ctx);
            Channel channel = ctx.channel();

            if (isStopped) {
                return;
            }

            InetSocketAddress remoteAddress = (InetSocketAddress) channel.remoteAddress();
            KinRpcAddress clientAddr = KinRpcAddress.of(remoteAddress.getHostName(), remoteAddress.getPort());
            ClientConnected clientConnected = ClientConnected.of(clientAddr);
            RpcMessageCallContext rpcMessageCallContext =
                    new RpcMessageCallContext(RpcEnv.this, clientAddr, channel, clientConnected);
            //分派
            dispatcher.post2All(rpcMessageCallContext);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            super.channelInactive(ctx);
            Channel channel = ctx.channel();

            if (isStopped) {
                return;
            }

            InetSocketAddress remoteAddress = (InetSocketAddress) channel.remoteAddress();
            KinRpcAddress clientAddr = KinRpcAddress.of(remoteAddress.getHostName(), remoteAddress.getPort());
            KinRpcAddress remoteBindAddr = clientAddr2RemoteBindAddr.remove(clientAddr);
            if (Objects.nonNull(remoteBindAddr)) {
                ClientDisconnected clientDisconnected = ClientDisconnected.of(remoteBindAddr);
                RpcMessageCallContext rpcMessageCallContext =
                        new RpcMessageCallContext(RpcEnv.this, remoteBindAddr, channel, clientDisconnected);
                //分派
                dispatcher.post2All(rpcMessageCallContext);
            }
        }
    }
}
