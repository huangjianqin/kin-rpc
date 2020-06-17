package org.kin.kinrpc.message.core;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import org.kin.framework.concurrent.ExecutionContext;
import org.kin.framework.concurrent.actor.Dispatcher;
import org.kin.framework.concurrent.actor.EventBasedDispatcher;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.framework.utils.SysUtils;
import org.kin.kinrpc.message.RpcEndpointThreadPool;
import org.kin.kinrpc.message.transport.TransportClient;
import org.kin.kinrpc.message.transport.domain.RpcEndpointAddress;
import org.kin.kinrpc.message.transport.protocol.RpcMessage;
import org.kin.kinrpc.transport.RpcEndpointHandler;
import org.kin.kinrpc.transport.domain.RpcAddress;
import org.kin.kinrpc.transport.protocol.RpcRequestProtocol;
import org.kin.kinrpc.transport.serializer.Serializer;
import org.kin.transport.netty.core.ServerTransportOption;
import org.kin.transport.netty.core.TransportOption;
import org.kin.transport.netty.core.protocol.ProtocolFactory;
import org.kin.transport.netty.core.statistic.InOutBoundStatisicService;
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
public class RpcEnv {
    private static final Logger log = LoggerFactory.getLogger(RpcEnv.class);

    static {
        ProtocolFactory.init(RpcRequestProtocol.class.getPackage().getName());
    }

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
    public ExecutionContext executors =
            ExecutionContext.forkjoin(SysUtils.getSuitableThreadNum(), "rpc-env", 5, "rpc-env-schedule");
    /** 事件调度 */
    private Dispatcher<String, RpcMessageCallContext> dispatcher;
    private final RpcAddress address;
    /** 序列化方式 */
    private final Serializer serializer;
    /** 通讯层是否支持压缩 */
    private final boolean compression;
    /** 服务器 */
    private RpcEndpointImpl rpcEndpoint;
    /** 标识是否stopped */
    private volatile boolean isStopped = false;
    /** 基于本rpc环境下的client */
    private Map<RpcAddress, TransportClient> clients = new ConcurrentHashMap<>();
    /**
     *
     */
    private Map<RpcAddress, OutBox> outBoxs = new ConcurrentHashMap<>();
    /**
     *
     */
    private Map<RpcEndpoint, RpcEndpointRef> endpoint2Ref = new ConcurrentHashMap<>();

    public RpcEnv(String host, int port, int parallelism, Serializer serializer, boolean compression) {
        this.address = RpcAddress.of(host, port);
        this.dispatcher = new EventBasedDispatcher<>(parallelism);
        this.serializer = serializer;
        this.compression = compression;
    }

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

        ServerTransportOption transportOption = TransportOption.server()
                .channelOption(ChannelOption.TCP_NODELAY, true)
                .channelOption(ChannelOption.SO_KEEPALIVE, true)
                .channelOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                //复用端口
                .channelOption(ChannelOption.SO_REUSEADDR, true)
                //receive窗口缓存6mb
                .channelOption(ChannelOption.SO_RCVBUF, 10 * 1024 * 1024)
                //send窗口缓存64kb
                .channelOption(ChannelOption.SO_SNDBUF, 64 * 1024)
                .transportHandler(rpcEndpoint);
        if (compression) {
            transportOption.compress();
        }

        try {
            rpcEndpoint.bind(transportOption, address);
        } catch (Exception e) {
            ExceptionUtils.log(e);
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

        dispatcher.register(name, rpcEndpoint, !rpcEndpoint.threadSafe());
        RpcEndpointRef endpointRef = new RpcEndpointRef(RpcEndpointAddress.of(address, name), this);
        endpoint2Ref.put(rpcEndpoint, endpointRef);
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
        if (Objects.nonNull(rpcEndpoint)) {
            rpcEndpoint.close();
        }
        for (RpcAddress rpcAddress : clients.keySet()) {
            removeClient(rpcAddress);
        }
        executors.shutdown();
        dispatcher.shutdown();
        RpcEndpointThreadPool.EXECUTORS.shutdown();
    }

    /**
     * 反序列化消息
     */
    public RpcMessage deserialize(byte[] data) {
        RpcMessage message;
        try {
            message = serializer.deserialize(data, RpcMessage.class);
            updateCurrentRpcEnv(this);

            InOutBoundStatisicService.instance().statisticReq(
                    message.getMessage().getClass().getName(), data.length
            );
            return message;
        } catch (IOException | ClassNotFoundException e) {
            ExceptionUtils.log(e);
        }
        return null;
    }

    /**
     * 序列化消息
     */
    public byte[] serialize(RpcMessage message) {
        try {
            message.setCreateTime(System.currentTimeMillis());
            byte[] data = serializer.serialize(message);

            InOutBoundStatisicService.instance().statisticReq(
                    message.getMessage().getClass().getName(), data.length
            );

            return data;
        } catch (IOException e) {
            ExceptionUtils.log(e);
        }
        return null;
    }

    //--------------------------------------------------------------------------------------------------------------------------------

    /**
     * 发送消息
     */
    public void send(RpcMessage message) {
        RpcEndpointAddress endpointAddress = message.getTo().getEndpointAddress();
        if (address.equals(endpointAddress.getRpcAddress())) {
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
        RpcAddress toRpcAddress = endpointAddress.getRpcAddress();
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
            outBoxs.remove(toRpcAddress);
            targetOutBox.stop();
        } else {
            targetOutBox.sendMessage(message);
        }
    }

    /**
     * 支持future的消息发送
     */
    public <R extends Serializable> RpcFuture<R> ask(RpcMessage message) {
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

    //--------------------------------------------------------------------------------------------------------------------------------

    /**
     * 分派并处理接受到的消息
     */
    public void postMessage(RpcMessage message) {
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
        return new RpcEndpointRef(
                RpcEndpointAddress.of(
                        RpcAddress.of(host, port), receiverName),
                this);
    }

    //----------------------------------------------------------------------------------------------------------------------

    /**
     * 获取客户端
     *
     * @param address remote地址
     */
    TransportClient getClient(RpcAddress address) {
        TransportClient transportClient;
        synchronized (clients) {
            if (isStopped) {
                return null;
            }
            if (clients.containsKey(address)) {
                return clients.get(address);
            }

            transportClient = new TransportClient(this, address.getHost(), address.getPort(), compression);
            transportClient.connect();
            clients.put(address, transportClient);
        }

        return transportClient;
    }

    /**
     * 移除启动了的客户端
     */
    void removeClient(RpcAddress address) {
        synchronized (clients) {
            if (isStopped) {
                return;
            }
            TransportClient client = clients.remove(address);
            client.stop();
        }
    }

    /**
     * 移除启动了的客户端
     */
    void removeOutBox(RpcAddress address) {
        if (isStopped) {
            return;
        }
        outBoxs.remove(address);
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * 该rpc环境的地址
     */
    public RpcAddress address() {
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
        return executors;
    }

    /**
     * dispatcher
     */
    public Dispatcher<String, RpcMessageCallContext> dispatcher() {
        return dispatcher;
    }

    //------------------------------------------------------------------------------------------------------------------
    private class RpcEndpointImpl extends RpcEndpointHandler {
        @Override
        protected final void handleRpcRequestProtocol(Channel channel, RpcRequestProtocol requestProtocol) {
            //处理接收到的消息
            if (isStopped) {
                return;
            }

            //反序列化内容
            byte[] data = requestProtocol.getReqContent();
            RpcMessage message = deserialize(data);
            if (Objects.isNull(message)) {
                return;
            }

            postMessage(channel, message);
        }
    }
}
