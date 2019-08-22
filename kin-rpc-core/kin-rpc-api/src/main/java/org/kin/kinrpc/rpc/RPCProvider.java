package org.kin.kinrpc.rpc;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.actor.ActorLike;
import org.kin.framework.concurrent.ThreadManager;
import org.kin.framework.utils.SysUtils;
import org.kin.kinrpc.common.URL;
import org.kin.kinrpc.rpc.exception.RateLimitException;
import org.kin.kinrpc.rpc.invoker.ProviderInvoker;
import org.kin.kinrpc.rpc.invoker.impl.ReflectProviderInvoker;
import org.kin.kinrpc.rpc.serializer.Serializer;
import org.kin.kinrpc.rpc.transport.ProviderHandler;
import org.kin.kinrpc.rpc.transport.domain.RPCRequest;
import org.kin.kinrpc.rpc.transport.domain.RPCResponse;
import org.kin.kinrpc.transport.netty.TransportOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

/**
 * Created by 健勤 on 2017/2/10.
 * 可以作为多个服务的Server
 */
public class RPCProvider extends ActorLike<RPCProvider> {
    private static final Logger log = LoggerFactory.getLogger(RPCProvider.class);
    //服务请求处理线程池
    private static ThreadManager THREADS = new ThreadManager(
            new ForkJoinPool(SysUtils.getSuitableThreadNum() * 2 - 1,
                    ForkJoinPool.defaultForkJoinWorkerThreadFactory,
                    null,
                    true
            ));

    static {
        JvmCloseCleaner.DEFAULT().add(() -> THREADS.shutdown());
    }


    private Map<String, ProviderInvokerWrapper> serviceMap = new ConcurrentHashMap<>();

    //占用端口
    private int port;
    //序列化方式
    private Serializer serializer;
    //底层的连接
    private ProviderHandler connection;
    //标识是否stopped
    private volatile boolean isStopped = false;

    public RPCProvider(int port, Serializer serializer) {
        /** 使用公用的线程池 */
        super(RPCThreadPool.THREADS);
        this.port = port;
        this.serializer = serializer;
    }

    /**
     * 支持动态添加服务
     */
    public void addService(URL url, Class<?> interfaceClass, Object service) {
        tell((rpcProvider) -> {
            if (!isStopped) {
                String serviceName = url.getServiceName();
                ReflectProviderInvoker invoker = new ReflectProviderInvoker(serviceName, service);
                invoker.init(interfaceClass);

                if (!serviceMap.containsKey(serviceName)) {
                    serviceMap.put(serviceName, new ProviderInvokerWrapper(url, invoker));
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
        if (isAlive()) {
            return !serviceMap.isEmpty();
        }

        return false;
    }

    public boolean isAlive() {
        return connection.isActive();
    }

    public Collection<URL> getAvailableServices() {
        return serviceMap.values().stream().map(ProviderInvokerWrapper::getUrl).collect(Collectors.toList());
    }

    public int getPort() {
        return port;
    }

    /**
     * 启动Server
     */
    public void start() {
        tell(rpcProvider -> {
            if (isStopped) {
                throw new RuntimeException("try start stopped provider");
            }
            log.info("provider(port={}) starting...", port);

            //启动连接
            this.connection = new ProviderHandler(new InetSocketAddress(this.port), this, serializer);
            TransportOption transportOption = TransportOption.create()
                    .channelOption(ChannelOption.TCP_NODELAY, true)
                    .channelOption(ChannelOption.SO_KEEPALIVE, true)
                    .channelOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .protocolHandler(connection);
            try {
                connection.bind(transportOption);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                System.exit(-1);
            }

            log.info("provider(port={}) started", port);
        });
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

            tell(rpcProvider2 -> {
                //最后关闭连接
                connection.close();
                log.info("server connection close successfully");
            });
        });
    }

    public void handleRequest(RPCRequest rpcRequest) {
        if (!isStopped) {
            tell(rpcProvider -> handleRPCRequest(rpcRequest));
        }
    }

    private void handleRPCRequest(RPCRequest rpcRequest) {
        if (!isStopped) {
            /** 处理请求 */
            log.debug("receive a request >>> " + rpcRequest);

            //提交线程池处理服务执行
            THREADS.execute(() -> {
                rpcRequest.setHandleTime(System.currentTimeMillis());
                String serviceName = rpcRequest.getServiceName();
                String methodName = rpcRequest.getMethod();
                Object[] params = rpcRequest.getParams();
                Channel channel = rpcRequest.getChannel();

                RPCResponse rpcResponse = new RPCResponse(rpcRequest.getRequestId(),
                        rpcRequest.getServiceName(), rpcRequest.getMethod());
                if (serviceMap.containsKey(serviceName)) {
                    ProviderInvoker invoker = serviceMap.get(serviceName).getInvoker();

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
                } else {
                    log.error("can not find service>>> {}", rpcRequest);
                    rpcResponse.setState(RPCResponse.State.ERROR, "unknown service");
                }
                rpcResponse.setCreateTime(System.currentTimeMillis());
                //write back to reference
                connection.resp(channel, rpcResponse);

                log.debug("finish handle request >>>>>>>>>" + System.lineSeparator() + rpcRequest + System.lineSeparator() + rpcResponse);
            });
        } else {
            /** 停止对外提供服务, 直接拒绝请求 */

            //创建RPCResponse,设置服务不可用请求重试标识,直接回发
            Channel channel = rpcRequest.getChannel();

            RPCResponse rpcResponse = new RPCResponse(rpcRequest.getRequestId(), rpcRequest.getServiceName(), rpcRequest.getMethod());
            rpcResponse.setState(RPCResponse.State.RETRY, "service unavailable");

            channel.write(rpcResponse);

            log.info("service shutdown, just reject request >>>>>>>>>" + rpcRequest);
        }
    }

    private class ProviderInvokerWrapper {
        private URL url;
        private ProviderInvoker invoker;

        public ProviderInvokerWrapper(URL url, ProviderInvoker invoker) {
            this.url = url;
            this.invoker = invoker;
        }

        //getter
        public URL getUrl() {
            return url;
        }

        public ProviderInvoker getInvoker() {
            return invoker;
        }
    }
}
