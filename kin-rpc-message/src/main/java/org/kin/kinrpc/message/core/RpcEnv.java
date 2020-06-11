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
import org.kin.kinrpc.message.api.RpcEndpoint;
import org.kin.kinrpc.message.api.RpcEndpointRef;
import org.kin.kinrpc.message.api.RpcMessageCallContext;
import org.kin.kinrpc.message.transport.TransportClient;
import org.kin.kinrpc.message.transport.domain.RpcEndpointAddress;
import org.kin.kinrpc.message.transport.protocol.RpcMessage;
import org.kin.kinrpc.transport.RpcEndpointHandler;
import org.kin.kinrpc.transport.domain.RpcAddress;
import org.kin.kinrpc.transport.protocol.RpcRequestProtocol;
import org.kin.kinrpc.transport.serializer.Serializer;
import org.kin.transport.netty.core.ServerTransportOption;
import org.kin.transport.netty.core.TransportOption;
import org.kin.transport.netty.core.statistic.InOutBoundStatisicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
    private Map<RpcAddress, OutBox> outBoxs = new ConcurrentHashMap<>();
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
     * @param name             rpc服务名
     * @param rpcEndpoint      rpc服务消息处理实现
     * @param enableConcurrent 是否支持并发处理消息
     */
    public final void register(String name, RpcEndpoint rpcEndpoint, boolean enableConcurrent) {
        if (isStopped) {
            return;
        }

        dispatcher.register(name, rpcEndpoint, enableConcurrent);
        endpoint2Ref.put(rpcEndpoint, new RpcEndpointRef(RpcEndpointAddress.of(address, name)));
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
        for (TransportClient transportClient : clients.values()) {
            transportClient.stop();
        }
        executors.shutdown();
        dispatcher.shutdown();
    }

    /**
     * 反序列化消息
     */
    public RpcMessage deserialize(byte[] data) {
        RpcMessage message;
        try {
            message = serializer.deserialize(data, RpcMessage.class);
            message.getFrom().updateRpcEnv(this);

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

    public TransportClient getClient(RpcAddress address) {
        TransportClient transportClient;
        synchronized (clients) {
            if (isStopped) {
                return null;
            }
            if (clients.containsKey(address)) {
                return clients.get(address);
            }

            transportClient = new TransportClient(this);
            transportClient.connect(address.getHost(), address.getPort(), compression);
            clients.put(address, transportClient);
        }

        return transportClient;
    }

    public void send(RpcMessage message) {
        RpcEndpointAddress endpointAddress = message.getTo().getEndpointAddress();
        if (endpointAddress.getRpcAddress().equals(address)) {
            //local
            postMessage0(endpointAddress.getName(), message);
        } else {
            post2OutBox(new OutBoxMessage(message));
        }
    }

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

    private void postMessage0(String name, RpcMessage message) {
        //TODO 可能需要统计一些消息处理具体信息
        RpcMessageCallContext rpcMessageCallContext =
                new RpcMessageCallContext(message.getFrom(), message.getMessage(), message.getRequestId(), message.getCreateTime());
        dispatcher.postMessage(name, rpcMessageCallContext);
    }

    //------------------------------------------------------------------------------------------------------------------
    public RpcAddress getAddress() {
        return address;
    }

    public RpcEndpointRef rpcEndpointRef(RpcEndpoint endpoint) {
        return endpoint2Ref.get(endpoint);
    }

    public ExecutionContext executors() {
        return executors;
    }

    //------------------------------------------------------------------------------------------------------------------
    private class RpcEndpointImpl extends RpcEndpointHandler {
        @Override
        protected final void handleRpcRequestProtocol(Channel channel, RpcRequestProtocol requestProtocol) {
            if (isStopped) {
                return;
            }

            //反序列化内容
            byte[] data = requestProtocol.getReqContent();
            RpcMessage message = deserialize(data);
            if (Objects.isNull(message)) {
                return;
            }

            postMessage0(message.getTo().getEndpointAddress().getName(), message);
        }
    }
}
