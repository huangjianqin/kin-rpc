//package org.kin.kinrpc.transport.kinrpc;
//
//import io.netty.channel.Channel;
//import io.netty.channel.ChannelOption;
//import org.kin.framework.utils.ExceptionUtils;
//import org.kin.framework.utils.ExtensionLoader;
//import org.kin.framework.utils.StringUtils;
//import org.kin.kinrpc.rpc.AsyncContext;
//import org.kin.kinrpc.rpc.executor.ExecutorFactory;
//import org.kin.kinrpc.rpc.Invoker;
//import org.kin.kinrpc.rpc.common.SslConfig;
//import org.kin.kinrpc.rpc.common.Url;
//import org.kin.kinrpc.rpc.exception.TpsLimitException;
//import org.kin.kinrpc.rpc.invoker.TpsLimitInvoker;
//import org.kin.kinrpc.serialization.Serialization;
//import org.kin.kinrpc.serialization.UnknownSerializationException;
//import org.kin.transport.netty.CompressionType;
//import org.kin.transport.netty.Transports;
//import org.kin.transport.netty.socket.SocketTransportOption;
//import org.kin.transport.netty.socket.protocol.ProtocolStatisicService;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.IOException;
//import java.net.InetSocketAddress;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.Map;
//import java.util.Objects;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.Executor;
//import java.util.concurrent.Future;
//import java.util.stream.Collectors;
//
///**
// * 可以作为多个服务的Server
// * Created by 健勤 on 2017/2/10.
// */
//@SuppressWarnings("rawtypes")
//public class KinRpcProvider {
//    private static final Logger log = LoggerFactory.getLogger(KinRpcProvider.class);
//
//    /** 服务, key -> serviceId, value -> provider invoker */
//    private final Map<Integer, Invoker> services = new ConcurrentHashMap<>();
//    /** 绑定地址 */
//    protected final InetSocketAddress address;
//    /** 序列化方式 */
//    private final Serialization serialization;
//    /** 底层的连接 */
//    private final ProviderHandler providerHandler;
//    /** 服务器启动配置 */
//    private final SocketTransportOption transportOption;
//    /**
//     * executorFactory
//     * <p>
//     * 注意: 如果kinrpc 服务中使用reference调用其他服务, 如果使用CompletableFuture进行异步处理, 则remote service返回结果的处理的executor则是在{@link org.kin.kinrpc.rpc.RpcThreadPool#EXECUTORS}处理
//     * 而不是在{@link ExecutorFactory}选择的{@link Executor}执行, 如果想要保持在同一{@link Executor}处理业务, 则需要block
//     */
//    private final ExecutorFactory executorFactory;
//    /** 是否stopped */
//    private volatile boolean stopped;
//
//    public KinRpcProvider(String host, int port, ExecutorFactory executorFactory, Serialization serialization, CompressionType compressionType, Map<ChannelOption, Object> options) {
//        this.executorFactory = executorFactory;
//        this.serialization = serialization;
//
//        if (StringUtils.isNotBlank(host)) {
//            this.address = new InetSocketAddress(host, port);
//        } else {
//            this.address = new InetSocketAddress(port);
//        }
//
//        this.providerHandler = new ProviderHandler();
//
//        SocketTransportOption.SocketServerTransportOptionBuilder builder = Transports.socket().server()
//                .channelOptions(options)
//                .protocolHandler(providerHandler)
//                .compress(compressionType);
//
//        String certPath = SslConfig.INSTANCE.getServerKeyCertChainPath();
//        String keyPath = SslConfig.INSTANCE.getServerPrivateKeyPath();
//
//        if (StringUtils.isNotBlank(certPath) && StringUtils.isNotBlank(keyPath)) {
//            builder.ssl(certPath, keyPath);
//        }
//
//        this.transportOption = builder.build();
//    }
//
//    /**
//     * 支持动态添加服务
//     */
//    public <T> void addService(Invoker<T> proxy) {
//        if (isAlive()) {
//            Url url = proxy.url();
//            String serviceKey = url.getServiceKey();
//            int serviceId = url.getServiceId();
//            Invoker<T> invoker = new TpsLimitInvoker<>(proxy);
//
//            if (!services.containsKey(serviceId)) {
//                services.put(serviceId, invoker);
//                log.info("provider(serviceKey={}, port={}) registered", serviceKey, getPort());
//            } else {
//                throw new IllegalStateException("service'" + serviceKey + "' has registered. can not register again");
//            }
//        }
//    }
//
//    /**
//     * 支持动态移除服务
//     */
//    public void disableService(Url url) {
//        services.remove(url.getServiceId());
//    }
//
//    /**
//     * 启动Server
//     */
//    public void bind() {
//        if (isShutdown()) {
//            throw new IllegalStateException("try start stopped provider");
//        }
//        log.info("provider(port={}) starting...", getPort());
//
//        //启动连接
//        providerHandler.bind(transportOption, address);
//
//        log.info("provider(port={}) started", getPort());
//    }
//
//    /**
//     * 默认每个服务关闭都需要关闭Server
//     * 但如果仍然有服务在此Server上提供服务,则仍然运行该Server
//     */
//    public void shutdown() {
//        if (!isShutdown()) {
//            stopped = true;
//            if (this.providerHandler == null) {
//                throw new IllegalStateException("Provider Server has not started");
//            }
//            providerHandler.close();
//            log.info("server(port= " + getPort() + ") connection close successfully");
//        }
//        log.warn("try shutdown stopped provider");
//    }
//
//
//    /**
//     * @return 是否shutdown
//     */
//    public boolean isShutdown() {
//        return stopped;
//    }
//
//    /**
//     * provider 空闲 shutdown
//     *
//     * @param other 额外的处理逻辑
//     */
//    public void idleShutdown(Runnable other) {
//        if (isShutdown()) {
//            return;
//        }
//        if (isBusy()) {
//            return;
//        }
//        if (isShutdown()) {
//            return;
//        }
//        shutdown();
//        if (Objects.nonNull(other)) {
//            other.run();
//        }
//    }
//
//    /**
//     * 处理rpc请求
//     * provider线程处理
//     */
//    private void handleRpcRequest(RpcRequest rpcRequest, Channel channel) {
//        //处理请求
//        log.debug("receive a request >>> " + rpcRequest);
//
//        if (isAlive()) {
//            String serviceKey = rpcRequest.getServiceKey();
//            int serviceId = serviceKey.hashCode();
//            String methodName = rpcRequest.getMethod();
//            Object[] params = rpcRequest.getParams();
//
//            RpcResponse rpcResponse = new RpcResponse(rpcRequest.getRequestId(),
//                    rpcRequest.getServiceKey(), rpcRequest.getMethod());
//            if (services.containsKey(serviceId)) {
//                Invoker invoker = services.get(serviceId);
//                Executor executor = executorFactory.executor(rpcRequest, channel);
//                executor.execute(() -> handlerRpcRequest0(executor, invoker, methodName, params, channel, rpcRequest, rpcResponse));
//            } else {
//                log.error("can not find service>>> {}", rpcRequest);
//                rpcResponse.setState(RpcResponse.State.RETRY, "unknown service");
//                //write back to reference
//                providerHandler.response(channel, rpcResponse);
//
//                log.debug("finish handle request >>>>>>>>>" + System.lineSeparator() + rpcRequest + System.lineSeparator() + rpcResponse);
//            }
//        } else {
//            //停止对外提供服务, 直接拒绝请求
//            //创建RpcResponse,设置服务不可用请求重试标识,直接回发
//            RpcResponse rpcResponse = new RpcResponse(rpcRequest.getRequestId(), rpcRequest.getServiceKey(), rpcRequest.getMethod());
//            rpcResponse.setState(RpcResponse.State.RETRY, "service unavailable");
//
//            channel.write(rpcResponse);
//
//            log.info("service shutdown, just reject request >>>>>>>>>" + rpcRequest);
//        }
//    }
//
//    /**
//     * 处理rpc请求
//     * 调用invoker处理rpc请求
//     */
//    private void handlerRpcRequest0(Executor executor, Invoker invoker, String methodName, Object[] params,
//                                    Channel channel, RpcRequest rpcRequest, RpcResponse rpcResponse) {
//        if (!channel.isActive()) {
//            throw new IllegalStateException("channel closed when handle request >>>>".concat(rpcRequest.toString()));
//        }
//
//        //提交线程池处理服务执行
//        rpcRequest.setHandleTime(System.currentTimeMillis());
//
//        //是否call timeout request
//        Object result = null;
//        if (rpcRequest.isCallTimeout()) {
//            //直接拒绝call timeout请求, 而不用调用service逻辑
//            rpcResponse.setState(RpcResponse.State.RETRY, "rpc call time out");
//        } else {
//            try {
//                result = invoker.invoke(methodName, params);
//                if (AsyncContext.asyncReturn()) {
//                    //provider service利用RpcServiceContext实现异步返回结果
//                    handlerServiceAsyncReturn(executor, AsyncContext.future(), channel, rpcRequest, rpcResponse);
//                    AsyncContext.reset();
//                    return;
//                }
//                if (result instanceof Future) {
//                    //返回结果是future
//                    handlerServiceAsyncReturn(executor, (Future) result, channel, rpcRequest, rpcResponse);
//                    return;
//                }
//                rpcResponse.setState(RpcResponse.State.SUCCESS, "success");
//            } catch (TpsLimitException e) {
//                rpcResponse.setState(RpcResponse.State.RETRY, "service tps limited, just reject");
//            } catch (Throwable throwable) {
//                //服务调用报错, 将异常信息返回
//                rpcResponse.setState(RpcResponse.State.ERROR, throwable.getMessage());
//                log.error(throwable.getMessage(), throwable);
//            }
//        }
//
//        responseRpcCall(result, channel, rpcRequest, rpcResponse);
//    }
//
//    /**
//     * 处理provider service异步返回结果
//     */
//    private void handlerServiceAsyncReturn(Executor executor, Future future, Channel channel, RpcRequest rpcRequest, RpcResponse rpcResponse) {
//        CompletableFuture.supplyAsync(() -> {
//            try {
//                return future.get();
//            } catch (Exception e) {
//                if (!(e instanceof InterruptedException)) {
//                    ExceptionUtils.throwExt(e);
//                }
//            }
//            return null;
//        }, executor).thenAcceptAsync(obj -> {
//            rpcResponse.setState(RpcResponse.State.SUCCESS, "success");
//
//            responseRpcCall(obj, channel, rpcRequest, rpcResponse);
//        }, executor).exceptionally(th -> {
//            if (th instanceof TpsLimitException) {
//                rpcResponse.setState(RpcResponse.State.RETRY, "service tps limited, just reject");
//            } else {
//                //服务调用报错, 将异常信息返回
//                rpcResponse.setState(RpcResponse.State.ERROR, th.getMessage());
//                log.error(th.getMessage(), th);
//            }
//            responseRpcCall(null, channel, rpcRequest, rpcResponse);
//            return null;
//        });
//    }
//
//    /**
//     * 响应rpc call
//     */
//    private void responseRpcCall(Object result, Channel channel, RpcRequest rpcRequest, RpcResponse rpcResponse) {
//        rpcResponse.setResult(result);
//        //write back to reference
//        providerHandler.response(channel, rpcResponse);
//
//        log.debug("finish handle request >>>>>>>>>" + System.lineSeparator() + rpcRequest + System.lineSeparator() + rpcResponse);
//    }
//
//    //--------------------------------------------------------------------------------------------------------------
//
//    /**
//     * 端口绑定没有断开 && 仍然可以对外提供服务(有服务绑定到该端口)
//     */
//    public boolean isBusy() {
//        return isAlive() && !services.isEmpty();
//    }
//
//    /**
//     * @return 端口绑定是否断开
//     */
//    public boolean isAlive() {
//        return !isShutdown() && providerHandler.isActive();
//    }
//
//    /**
//     * @return 可提供的服务
//     */
//    public Collection<Url> getAvailableServices() {
//        if (!isAlive()) {
//            return Collections.emptyList();
//        }
//        return services.values().stream().map(Invoker::url).collect(Collectors.toList());
//    }
//
//    //getter
//    public String getHost() {
//        return address.getHostName();
//    }
//
//    public int getPort() {
//        return address.getPort();
//    }
//
//    public Serialization getSerialization() {
//        return serialization;
//    }
//
//    /**
//     * rpc消息传输逻辑处理
//     */
//    private class ProviderHandler extends KinRpcEndpointHandler {
//        /**
//         * write back response
//         */
//        public void response(Channel channel, RpcResponse rpcResponse) {
//            byte[] data;
//            try {
//                data = serialization.serialize(rpcResponse);
//            } catch (IOException e) {
//                log.error(e.getMessage(), e);
//                rpcResponse.setState(RpcResponse.State.ERROR, e.getMessage());
//                rpcResponse.setResult(null);
//                try {
//                    data = serialization.serialize(rpcResponse);
//                } catch (IOException e1) {
//                    log.error(e1.getMessage(), e1);
//                    return;
//                }
//            }
//
//            KinRpcResponseProtocol rpcResponseProtocol = KinRpcResponseProtocol.create(rpcResponse.getRequestId(), (byte) serialization.type(), data);
//            channel.writeAndFlush(rpcResponseProtocol);
//
//            ProtocolStatisicService.instance().statisticResp(
//                    rpcResponse.getServiceKey() + "-" + rpcResponse.getMethod(), Objects.nonNull(data) ? data.length : 0
//            );
//        }
//
//        @Override
//        protected void handleRpcRequestProtocol(Channel channel, KinRpcRequestProtocol requestProtocol) {
//            if (isShutdown()) {
//                return;
//            }
//            long requestId = requestProtocol.getRequestId();
//            byte serializationType = requestProtocol.getSerialization();
//            byte[] data = requestProtocol.getReqContent();
//
//            RpcRequest rpcRequest;
//            try {
//                //request的序列化类型
//                Serialization serialization = ExtensionLoader.getExtension(Serialization.class, serializationType);
//                if (Objects.isNull(serialization)) {
//                    //未知序列化类型
//                    throw new UnknownSerializationException(serializationType);
//                }
//
//                rpcRequest = serialization.deserialize(data, RpcRequest.class);
//
//                ProtocolStatisicService.instance().statisticReq(
//                        rpcRequest.getServiceKey() + "-" + rpcRequest.getMethod(), data.length
//                );
//
//                rpcRequest.setEventTime(System.currentTimeMillis());
//            } catch (Exception e) {
//                RpcResponse rpcResponse = RpcResponse.respWithError(requestId, e.getMessage());
//                response(channel, rpcResponse);
//
//                log.error(e.getMessage(), e);
//                return;
//            }
//
//            //直接io线程处理
//            handleRpcRequest(rpcRequest, channel);
//        }
//    }
//}
