package org.kin.kinrpc.transport.kinrpc;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import org.kin.framework.concurrent.actor.PinnedThreadSafeHandler;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.rpc.Invoker;
import org.kin.kinrpc.rpc.RpcThreadPool;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.rpc.exception.RateLimitException;
import org.kin.kinrpc.rpc.invoker.RateLimitInvoker;
import org.kin.kinrpc.serializer.Serializer;
import org.kin.kinrpc.serializer.Serializers;
import org.kin.kinrpc.serializer.UnknownSerializerException;
import org.kin.transport.netty.CompressionType;
import org.kin.transport.netty.Transports;
import org.kin.transport.netty.socket.protocol.ProtocolStatisicService;
import org.kin.transport.netty.socket.server.SocketServerTransportOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 可以作为多个服务的Server
 * Created by 健勤 on 2017/2/10.
 */
public class KinRpcProvider extends PinnedThreadSafeHandler<KinRpcProvider> {
    private static final Logger log = LoggerFactory.getLogger(KinRpcProvider.class);

    /** 服务 */
    private final Map<String, InvokerWrapper> services = new ConcurrentHashMap<>();

    /** 绑定地址 */
    protected final InetSocketAddress address;
    /** 序列化方式 */
    private final Serializer serializer;
    /** 底层的连接 */
    private final ProviderHandler providerHandler;
    /** 服务器启动配置 */
    private final SocketServerTransportOption transportOption;
    /** 标识是否stopped */
    private volatile boolean isStopped = false;

    public KinRpcProvider(String host, int port, Serializer serializer, CompressionType compressionType) {
        super(RpcThreadPool.providerWorkers());
        this.serializer = serializer;

        if (StringUtils.isNotBlank(host)) {
            this.address = new InetSocketAddress(host, port);
        } else {
            this.address = new InetSocketAddress(port);
        }

        this.providerHandler = new ProviderHandler();
        this.transportOption = Transports.socket().server()
                .channelOption(ChannelOption.TCP_NODELAY, true)
                .channelOption(ChannelOption.SO_KEEPALIVE, true)
                .channelOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                //复用端口
                .channelOption(ChannelOption.SO_REUSEADDR, true)
                //receive窗口缓存6mb
                .channelOption(ChannelOption.SO_RCVBUF, 10 * 1024 * 1024)
                //send窗口缓存64kb
                .channelOption(ChannelOption.SO_SNDBUF, 64 * 1024)
                .protocolHandler(providerHandler)
                .compress(compressionType)
                .build();
    }

    /**
     * 支持动态添加服务
     */
    public <T> void addService(Invoker<T> proxy) {
        handle((rpcProvider) -> {
            if (isAlive()) {
                Url url = proxy.url();
                String serviceName = url.getServiceName();
                Invoker<T> invoker = new RateLimitInvoker<>(proxy);

                if (!services.containsKey(serviceName)) {
                    services.put(serviceName, new InvokerWrapper(invoker));
                    log.info("provider(serviceName={}, port={}) registered", serviceName, getPort());
                } else {
                    throw new IllegalStateException("service'" + serviceName + "' has registered. can not register again");
                }
            }
        });
    }

    /**
     * 支持动态移除服务
     */
    public void disableService(Url url) {
        handle(rpcProvider -> {
            String serviceName = url.getServiceName();
            services.remove(serviceName);
        });
    }

    /**
     * 启动Server
     */
    public void bind() {
        if (isStopped) {
            throw new IllegalStateException("try start stopped provider");
        }
        handle(rpcProvider -> {
            log.info("provider(port={}) starting...", getPort());

            //启动连接
            providerHandler.bind(transportOption, address);

            log.info("provider(port={}) started", getPort());
        });
    }

    /**
     * 默认每个服务关闭都需要关闭Server
     * 但如果仍然有服务在此Server上提供服务,则仍然运行该Server
     */
    public void shutdown() {
        if (!isStopped) {
            shutdownNow();
        }
        log.warn("try shutdown stopped provider");
    }

    /**
     * 不管3721,马上stop
     */
    public void shutdownNow() {
        handle(rpcProvider1 -> {
            if (isStopped) {
                return;
            }
            if (this.providerHandler == null) {
                throw new IllegalStateException("Provider Server has not started");
            }
            log.info("server(port= " + getPort() + ") shutdowning...");
            isStopped = true;

            //让所有请求都拒绝返回时, 才关闭channel
            handle(rpcProvider2 -> {
                //最后关闭连接
                providerHandler.close();
                log.info("server connection close successfully");
            });
        });
    }

    /**
     * 处理rpc请求
     * provider线程处理
     */
    private void handleRpcRequest(RpcRequest rpcRequest, Channel channel) {
        //处理请求
        log.debug("receive a request >>> " + rpcRequest);
        //提交线程池处理服务执行
        rpcRequest.setHandleTime(System.currentTimeMillis());

        if (isAlive()) {
            String serviceName = rpcRequest.getServiceName();
            String methodName = rpcRequest.getMethod();
            Object[] params = rpcRequest.getParams();

            RpcResponse rpcResponse = new RpcResponse(rpcRequest.getRequestId(),
                    rpcRequest.getServiceName(), rpcRequest.getMethod());
            if (services.containsKey(serviceName)) {
                InvokerWrapper invokerWrapper = services.get(serviceName);
                if (invokerWrapper.parallelism) {
                    //并发处理
                    RpcThreadPool.providerWorkers().execute(() -> handlerRpcRequest0(invokerWrapper.getInvoker(), methodName, params, channel, rpcRequest, rpcResponse));
                } else {
                    //同一invoker同一线程处理
                    invokerWrapper.handle(iw -> handlerRpcRequest0(invokerWrapper.getInvoker(), methodName, params, channel, rpcRequest, rpcResponse));
                }
            } else {
                log.error("can not find service>>> {}", rpcRequest);
                rpcResponse.setState(RpcResponse.State.ERROR, "unknown service");
                rpcResponse.setCreateTime(System.currentTimeMillis());
                //write back to reference
                providerHandler.response(channel, rpcResponse);

                log.debug("finish handle request >>>>>>>>>" + System.lineSeparator() + rpcRequest + System.lineSeparator() + rpcResponse);
            }
        } else {
            //停止对外提供服务, 直接拒绝请求

            //创建RpcResponse,设置服务不可用请求重试标识,直接回发
            RpcResponse rpcResponse = new RpcResponse(rpcRequest.getRequestId(), rpcRequest.getServiceName(), rpcRequest.getMethod());
            rpcResponse.setState(RpcResponse.State.RETRY, "service unavailable");

            channel.write(rpcResponse);

            log.info("service shutdown, just reject request >>>>>>>>>" + rpcRequest);
        }
    }

    /**
     * 处理rpc请求
     * 调用invoker处理rpc请求
     */
    private void handlerRpcRequest0(Invoker invoker, String methodName, Object[] params,
                                    Channel channel, RpcRequest rpcRequest, RpcResponse rpcResponse) {
        Object result = null;
        try {
            result = invoker.invoke(methodName, params);
            rpcResponse.setState(RpcResponse.State.SUCCESS, "success");
        } catch (RateLimitException e) {
            rpcResponse.setState(RpcResponse.State.RETRY, "service rate limited, just reject");
        } catch (Throwable throwable) {
            //服务调用报错, 将异常信息返回
            rpcResponse.setState(RpcResponse.State.ERROR, throwable.getMessage());
            log.error(throwable.getMessage(), throwable);
        }

        rpcResponse.setResult(result);
        rpcResponse.setCreateTime(System.currentTimeMillis());
        //write back to reference
        providerHandler.response(channel, rpcResponse);

        log.debug("finish handle request >>>>>>>>>" + System.lineSeparator() + rpcRequest + System.lineSeparator() + rpcResponse);
    }

    //--------------------------------------------------------------------------------------------------------------
    public boolean isBusy() {
        return isAlive() && !services.isEmpty();
    }

    public boolean isAlive() {
        return !isStopped && providerHandler.isActive();
    }

    public Collection<Url> getAvailableServices() {
        if (!isAlive()) {
            return Collections.emptyList();
        }
        return services.values().stream().map(InvokerWrapper::getUrl).collect(Collectors.toList());
    }

    public String getHost() {
        return address.getHostName();
    }

    public int getPort() {
        return address.getPort();
    }

    public Serializer getSerializer() {
        return serializer;
    }

    //--------------------------------------------------------------------------------------------------------------

    /**
     * 类actor的invoker
     */
    private class InvokerWrapper extends PinnedThreadSafeHandler<InvokerWrapper> {
        /** 包装的invoker */
        private final Invoker invoker;
        /** invoker invoke方式, 并发或者类actor */
        private final boolean parallelism;

        public InvokerWrapper(Invoker invoker) {
            super(RpcThreadPool.providerWorkers());
            this.invoker = invoker;
            this.parallelism = Boolean.parseBoolean(invoker.url().getParam(Constants.PARALLELISM_KEY));
        }

        //getter
        public Url getUrl() {
            return invoker.url();
        }

        public Invoker getInvoker() {
            return invoker;
        }

        public boolean isParallelism() {
            return parallelism;
        }
    }

    /**
     * rpc消息传输逻辑处理
     */
    private class ProviderHandler extends KinRpcEndpointHandler {
        public void response(Channel channel, RpcResponse rpcResponse) {
            byte[] data;
            try {
                data = serializer.serialize(rpcResponse);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                rpcResponse.setState(RpcResponse.State.ERROR, e.getMessage());
                rpcResponse.setResult(null);
                try {
                    data = serializer.serialize(rpcResponse);
                } catch (IOException e1) {
                    log.error(e1.getMessage(), e1);
                    return;
                }
            }

            KinRpcResponseProtocol rpcResponseProtocol = KinRpcResponseProtocol.create(rpcResponse.getRequestId(), (byte) serializer.type(), data);
            channel.writeAndFlush(rpcResponseProtocol);


            ProtocolStatisicService.instance().statisticResp(
                    rpcResponse.getServiceName() + "-" + rpcResponse.getMethod(), Objects.nonNull(data) ? data.length : 0
            );
        }

        @Override
        protected void handleRpcRequestProtocol(Channel channel, KinRpcRequestProtocol requestProtocol) {
            long requestId = requestProtocol.getRequestId();
            byte serializerType = requestProtocol.getSerializer();
            byte[] data = requestProtocol.getReqContent();

            RpcRequest rpcRequest;
            try {
                //request的序列化类型
                Serializer serializer = Serializers.getSerializer(serializerType);
                if (Objects.isNull(serializer)) {
                    //未知序列化类型
                    throw new UnknownSerializerException(serializerType);
                }

                rpcRequest = serializer.deserialize(data, RpcRequest.class);

                ProtocolStatisicService.instance().statisticReq(
                        rpcRequest.getServiceName() + "-" + rpcRequest.getMethod(), data.length
                );

                rpcRequest.setEventTime(System.currentTimeMillis());
            } catch (Exception e) {
                RpcResponse rpcResponse = RpcResponse.respWithError(requestId, e.getMessage());
                response(channel, rpcResponse);

                log.error(e.getMessage(), e);
                return;
            }

            RpcRequest finalRpcRequest = rpcRequest;
            KinRpcProvider.this.handle(rpcProvider -> handleRpcRequest(finalRpcRequest, channel));
        }
    }
}
