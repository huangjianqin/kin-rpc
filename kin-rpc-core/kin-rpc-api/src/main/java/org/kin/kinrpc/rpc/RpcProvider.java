package org.kin.kinrpc.rpc;

import com.google.common.util.concurrent.RateLimiter;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import org.kin.framework.concurrent.actor.PinnedThreadSafeHandler;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.rpc.exception.RateLimitException;
import org.kin.kinrpc.rpc.invoker.ProviderInvoker;
import org.kin.kinrpc.rpc.invoker.impl.JavassistProviderInvoker;
import org.kin.kinrpc.rpc.invoker.impl.ReflectProviderInvoker;
import org.kin.kinrpc.rpc.transport.RpcRequest;
import org.kin.kinrpc.rpc.transport.RpcResponse;
import org.kin.kinrpc.transport.RpcEndpointHandler;
import org.kin.kinrpc.transport.protocol.RpcRequestProtocol;
import org.kin.kinrpc.transport.protocol.RpcResponseProtocol;
import org.kin.kinrpc.transport.serializer.Serializer;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by 健勤 on 2017/2/10.
 * 可以作为多个服务的Server
 */
public class RpcProvider extends PinnedThreadSafeHandler<RpcProvider> {
    private static final Logger log = LoggerFactory.getLogger(RpcProvider.class);

    /** 服务 */
    private Map<String, ProviderInvokerWrapper> serviceMap = new ConcurrentHashMap<>();

    /** 占用主机名 */
    private final String host;
    /** 占用端口 */
    private final int port;
    /** 绑定地址 */
    protected InetSocketAddress address;
    /** 序列化方式 */
    private final Serializer serializer;
    /** 底层的连接 */
    private ProviderHandler providerHandler;
    /** 标识是否stopped */
    private volatile boolean isStopped = false;
    /** 是否使用字节码技术 */
    private final boolean isByteCodeInvoke;
    /** 服务器启动配置 */
    private SocketServerTransportOption transportOption;
    /** 流控 */
    private RateLimiter rateLimiter = RateLimiter.create(Constants.SERVER_REQUEST_THRESHOLD);

    public RpcProvider(String host, int port, Serializer serializer, boolean isByteCodeInvoke, boolean compression) {
        super(RpcThreadPool.PROVIDER_WORKER);
        this.host = host;
        this.port = port;
        this.serializer = serializer;
        this.isByteCodeInvoke = isByteCodeInvoke;

        if (StringUtils.isNotBlank(host)) {
            this.address = new InetSocketAddress(this.host, this.port);
        } else {
            this.address = new InetSocketAddress(this.port);
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
                .protocolHandler(providerHandler);
        if (compression) {
            this.transportOption.compress();
        }
    }

    /**
     * 支持动态添加服务
     */
    public void addService(Url url, Class<?> interfaceClass, Object service) {
        handle((rpcProvider) -> {
            if (isAlive()) {
                String serviceName = url.getServiceName();
                ProviderInvoker invoker;

                int rate = Integer.parseInt(url.getParam(Constants.RATE_KEY));
                if (isByteCodeInvoke) {
                    //使用javassist调用服务类接口方法
                    invoker = new JavassistProviderInvoker(serviceName, service, interfaceClass, rate);
                } else {
                    //使用反射调用服务类接口方法
                    invoker = new ReflectProviderInvoker(serviceName, service, interfaceClass, rate);
                }

                if (!serviceMap.containsKey(serviceName)) {
                    serviceMap.put(serviceName, new ProviderInvokerWrapper(url, invoker));
                    log.info("provider(serviceName={}, port={}) registered", serviceName, port);
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
            serviceMap.remove(serviceName);
        });
    }

    public boolean isBusy() {
        return isAlive() && !serviceMap.isEmpty();
    }

    public boolean isAlive() {
        return !isStopped && providerHandler.isActive();
    }

    public Collection<Url> getAvailableServices() {
        if (!isAlive()) {
            return Collections.emptyList();
        }
        return serviceMap.values().stream().map(ProviderInvokerWrapper::getUrl).collect(Collectors.toList());
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    /**
     * 启动Server
     */
    public void start() {
        if (isStopped) {
            throw new RuntimeException("try start stopped provider");
        }
        log.info("provider(port={}) starting...", port);

        //启动连接
        providerHandler.bind(transportOption, address);

        log.info("provider(port={}) started", port);
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
            log.info("server(port= " + port + ") shutdowning...");
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
            if (serviceMap.containsKey(serviceName)) {
                ProviderInvokerWrapper invokerWrapper = serviceMap.get(serviceName);
                if (invokerWrapper.parallelism) {
                    //并发处理
                    RpcThreadPool.PROVIDER_WORKER.execute(() -> handlerRpcRequest0(invokerWrapper.getInvoker(), methodName, params, channel, rpcRequest, rpcResponse));
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
    private void handlerRpcRequest0(ProviderInvoker invoker, String methodName, Object[] params,
                                    Channel channel, RpcRequest rpcRequest, RpcResponse rpcResponse) {
        Object result = null;
        try {
            result = invoker.invoke(methodName, false, params);
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

    private class ProviderInvokerWrapper extends PinnedThreadSafeHandler<ProviderInvokerWrapper> {
        private Url url;
        private ProviderInvoker invoker;
        private boolean parallelism;

        public ProviderInvokerWrapper(Url url, ProviderInvoker invoker) {
            super(RpcThreadPool.PROVIDER_WORKER);
            this.url = url;
            this.invoker = invoker;
            this.parallelism = Boolean.parseBoolean(url.getParam(Constants.PARALLELISM_KEY));
        }

        //getter
        public Url getUrl() {
            return url;
        }

        public ProviderInvoker getInvoker() {
            return invoker;
        }

        public boolean isParallelism() {
            return parallelism;
        }
    }

    private class ProviderHandler extends RpcEndpointHandler {
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

            RpcResponseProtocol rpcResponseProtocol = RpcResponseProtocol.create(data);
            channel.writeAndFlush(rpcResponseProtocol);


            ProtocolStatisicService.instance().statisticResp(
                    rpcResponse.getServiceName() + "-" + rpcResponse.getMethod(), data.length
            );
        }

        @Override
        protected void handleRpcRequestProtocol(Channel channel, RpcRequestProtocol requestProtocol) {
            byte[] data = requestProtocol.getReqContent();

            RpcRequest rpcRequest;
            try {
                rpcRequest = serializer.deserialize(data, RpcRequest.class);

                ProtocolStatisicService.instance().statisticReq(
                        rpcRequest.getServiceName() + "-" + rpcRequest.getMethod(), data.length
                );

                rpcRequest.setEventTime(System.currentTimeMillis());
            } catch (IOException | ClassNotFoundException e) {
                log.error(e.getMessage(), e);
                return;
            }

            //简单地添加到任务队列交由上层的线程池去完成服务调用
            //流控
            if (!rateLimiter.tryAcquire()) {
                RpcResponse rpcResponse = RpcResponse.respWithError(rpcRequest, "server rate limited, just reject");
                rpcRequest.setHandleTime(System.currentTimeMillis());
                response(channel, rpcResponse);
                return;
            }

            RpcRequest finalRpcRequest = rpcRequest;
            RpcProvider.this.handle(rpcProvider -> handleRpcRequest(finalRpcRequest, channel));
        }
    }
}
