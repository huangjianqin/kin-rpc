package org.kin.kinrpc.rpc;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.actor.ActorLike;
import org.kin.framework.concurrent.ThreadManager;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.common.Constants;
import org.kin.kinrpc.common.URL;
import org.kin.kinrpc.rpc.exception.RateLimitException;
import org.kin.kinrpc.rpc.invoker.ProviderInvoker;
import org.kin.kinrpc.rpc.invoker.impl.JavassistProviderInvoker;
import org.kin.kinrpc.rpc.invoker.impl.ReflectProviderInvoker;
import org.kin.kinrpc.rpc.serializer.Serializer;
import org.kin.kinrpc.rpc.transport.ProviderHandler;
import org.kin.kinrpc.rpc.transport.domain.RPCRequest;
import org.kin.kinrpc.rpc.transport.domain.RPCResponse;
import org.kin.transport.netty.core.ServerTransportOption;
import org.kin.transport.netty.core.TransportOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class RPCProvider extends ActorLike<RPCProvider> {
    private static final Logger log = LoggerFactory.getLogger(RPCProvider.class);
    //服务请求处理线程池
    /**
     * 可以在加载类RPCProvider前修改RPC.parallelism来修改RPCProvider的并发数
     */
    private static ThreadManager EXECUTORS =
            ThreadManager.asyncForkjoin(KinRPC.PARALLELISM, "rpc-", 2, "rpc-schedule-");
    static {
        JvmCloseCleaner.DEFAULT().add(() -> EXECUTORS.shutdown());
    }

    /** 服务 */
    private Map<String, ProviderInvokerWrapper> serviceMap = new ConcurrentHashMap<>();

    /** 占用主机名 */
    private final String host;
    /** 占用端口 */
    private final int port;
    /** 序列化方式 */
    private final Serializer serializer;
    /** 底层的连接 */
    private ProviderHandler connection;
    /** 标识是否stopped */
    private volatile boolean isStopped = false;
    /** 是否使用字节码技术 */
    private final boolean isByteCodeInvoke;
    /** 是否压缩 */
    private final boolean compression;

    public RPCProvider(String host, int port, Serializer serializer, boolean isByteCodeInvoke, boolean compression) {
        super(EXECUTORS);
        this.host = host;
        this.port = port;
        this.serializer = serializer;
        this.isByteCodeInvoke = isByteCodeInvoke;
        this.compression = compression;
    }

    /**
     * 支持动态添加服务
     */
    public void addService(URL url, Class<?> interfaceClass, Object service) {
        tell((rpcProvider) -> {
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
    public void disableService(URL url) {
        tell(rpcProvider -> {
            String serviceName = url.getServiceName();
            serviceMap.remove(serviceName);
        });
    }

    public boolean isBusy() {
        return isAlive() && !serviceMap.isEmpty();
    }

    public boolean isAlive() {
        return !isStopped && connection.isActive();
    }

    public Collection<URL> getAvailableServices() {
        if (!isAlive()) {
            return Collections.emptyList();
        }
        return serviceMap.values().stream().map(ProviderInvokerWrapper::getUrl).collect(Collectors.toList());
    }

    public int getPort() {
        return port;
    }

    /**
     * 启动Server
     */
    public void start() throws Exception {
        if (isStopped) {
            throw new RuntimeException("try start stopped provider");
        }
        log.info("provider(port={}) starting...", port);

        //启动连接
        InetSocketAddress inetAddress;
        if (StringUtils.isNotBlank(host)) {
            inetAddress = new InetSocketAddress(this.host, this.port);
        } else {
            inetAddress = new InetSocketAddress(this.port);
        }

        this.connection = new ProviderHandler(inetAddress, this, serializer);
        ServerTransportOption transportOption = TransportOption.server()
                .channelOption(ChannelOption.TCP_NODELAY, true)
                .channelOption(ChannelOption.SO_KEEPALIVE, true)
                .channelOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .protocolHandler(connection);
        if (compression) {
            transportOption.compress();
        }
        connection.bind(transportOption);

        log.info("provider(port={}) started", port);
    }

    /**
     * 默认每个服务关闭都需要关闭Server
     * 但如果仍然有服务在此Server上提供服务,则仍然运行该Server
     */
    public void shutdown() {
        if (isStopped) {
            throw new IllegalStateException("try shutdown stopped provider");
        }
        shutdownNow();
    }

    /**
     * 不管3721,马上stop
     */
    public void shutdownNow() {
        tell(rpcProvider1 -> {
            if (isStopped) {
                return;
            }
            if (this.connection == null) {
                throw new IllegalStateException("Provider Server has not started");
            }
            log.info("server(port= " + port + ") shutdowning...");
            isStopped = true;

            //让所有请求都拒绝返回时, 才关闭channel
            tell(rpcProvider2 -> {
                //最后关闭连接
                connection.close();
                log.info("server connection close successfully");
            });
        });
    }

    /**
     * 处理rpc请求
     * 对外接口
     */
    public void handleRequest(RPCRequest rpcRequest) {
        if (isAlive()) {
            tell(rpcProvider -> handleRPCRequest(rpcRequest));
        }
    }

    /**
     * 处理rpc请求
     * provider线程处理
     */
    private void handleRPCRequest(RPCRequest rpcRequest) {
        if (isAlive()) {
            //处理请求
            log.debug("receive a request >>> " + rpcRequest);

            //提交线程池处理服务执行
            rpcRequest.setHandleTime(System.currentTimeMillis());
            String serviceName = rpcRequest.getServiceName();
            String methodName = rpcRequest.getMethod();
            Object[] params = rpcRequest.getParams();
            Channel channel = rpcRequest.getChannel();

            RPCResponse rpcResponse = new RPCResponse(rpcRequest.getRequestId(),
                    rpcRequest.getServiceName(), rpcRequest.getMethod());
            if (serviceMap.containsKey(serviceName)) {
                ProviderInvokerWrapper invokerWrapper = serviceMap.get(serviceName);
                if (invokerWrapper.parallelism) {
                    //并发处理
                    EXECUTORS.execute(() -> handlerRPCRequest0(invokerWrapper.getInvoker(), methodName, params, channel, rpcRequest, rpcResponse));
                } else {
                    //同一invoker同一线程处理
                    invokerWrapper.tell(iw -> handlerRPCRequest0(invokerWrapper.getInvoker(), methodName, params, channel, rpcRequest, rpcResponse));
                }
            } else {
                log.error("can not find service>>> {}", rpcRequest);
                rpcResponse.setState(RPCResponse.State.ERROR, "unknown service");
                rpcResponse.setCreateTime(System.currentTimeMillis());
                //write back to reference
                connection.resp(channel, rpcResponse);

                log.debug("finish handle request >>>>>>>>>" + System.lineSeparator() + rpcRequest + System.lineSeparator() + rpcResponse);
            }
        } else {
            //停止对外提供服务, 直接拒绝请求

            //创建RPCResponse,设置服务不可用请求重试标识,直接回发
            Channel channel = rpcRequest.getChannel();

            RPCResponse rpcResponse = new RPCResponse(rpcRequest.getRequestId(), rpcRequest.getServiceName(), rpcRequest.getMethod());
            rpcResponse.setState(RPCResponse.State.RETRY, "service unavailable");

            channel.write(rpcResponse);

            log.info("service shutdown, just reject request >>>>>>>>>" + rpcRequest);
        }
    }

    /**
     * 处理rpc请求
     * 调用invoker处理rpc请求
     */
    private void handlerRPCRequest0(ProviderInvoker invoker, String methodName, Object[] params,
                                    Channel channel, RPCRequest rpcRequest, RPCResponse rpcResponse) {
        Object result = null;
        try {
            result = invoker.invoke(methodName, false, params);
            rpcResponse.setState(RPCResponse.State.SUCCESS, "success");
        } catch (RateLimitException e) {
            rpcResponse.setState(RPCResponse.State.RETRY, "service rate limited, just reject");
        } catch (Throwable throwable) {
            //服务调用报错, 将异常信息返回
            rpcResponse.setState(RPCResponse.State.ERROR, throwable.getMessage());
            log.error(throwable.getMessage(), throwable);
        }

        rpcResponse.setResult(result);
        rpcResponse.setCreateTime(System.currentTimeMillis());
        //write back to reference
        connection.resp(channel, rpcResponse);

        log.debug("finish handle request >>>>>>>>>" + System.lineSeparator() + rpcRequest + System.lineSeparator() + rpcResponse);
    }

    private static class ProviderInvokerWrapper extends ActorLike<ProviderInvokerWrapper> {
        private URL url;
        private ProviderInvoker invoker;
        private boolean parallelism;

        public ProviderInvokerWrapper(URL url, ProviderInvoker invoker) {
            super(EXECUTORS);
            this.url = url;
            this.invoker = invoker;
            this.parallelism = Boolean.parseBoolean(url.getParam(Constants.PARALLELISM_KEY));
        }

        //getter
        public URL getUrl() {
            return url;
        }

        public ProviderInvoker getInvoker() {
            return invoker;
        }

        public boolean isParallelism() {
            return parallelism;
        }
    }
}
