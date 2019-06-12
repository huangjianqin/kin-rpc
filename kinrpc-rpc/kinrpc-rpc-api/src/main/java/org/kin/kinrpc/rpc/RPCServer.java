package org.kin.kinrpc.rpc;

import io.netty.channel.Channel;
import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.concurrent.ThreadManager;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.kinrpc.rpc.invoker.ProviderInvoker;
import org.kin.kinrpc.rpc.invoker.impl.JavaProviderInvoker;
import org.kin.kinrpc.transport.Connection;
import org.kin.kinrpc.transport.rpc.ProviderConnection;
import org.kin.kinrpc.transport.rpc.RPCRequestHandler;
import org.kin.kinrpc.transport.rpc.domain.RPCRequest;
import org.kin.kinrpc.transport.rpc.domain.RPCResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by 健勤 on 2017/2/10.
 * 可以作为多个服务的Server
 */
public class RPCServer implements RPCRequestHandler {
    private static final Logger log = LoggerFactory.getLogger("invoker");

    //只有get的时候没有同步,其余都同步了
    //大部分情况下get调用的频率比其他方法都多,没有必要使用同步容器,提供一丢丢性能
    private final Map<String, ProviderInvoker> serviceMap = new HashMap<String, ProviderInvoker>();
    //各种服务请求处理的线程池
    private final ThreadManager threads;
    //保证RPCRequest按请求顺序进队
    private final ThreadManager singleThread = new ThreadManager(Executors.newSingleThreadExecutor());
    //RPCRequest队列,所有连接该Server的consumer发送的request都put进这个队列
    //然后由一个专门的线程不断地get,再提交到线程池去处理
    //本质上是生产者-消费者模式
    private final BlockingQueue<RPCRequest> requestsQueue = new LinkedBlockingQueue<RPCRequest>();

    //server配置
    private final int port;
    private int threadNum;
    //底层的连接
    private Connection connection;
    //扫描RPCRequest的线程
    private ScanRequestsThread scanRequestsThread;
    //用于标识该Server是否stopped
    private boolean isStopped = false;

    public RPCServer(int port, int threadNum) {
        this.port = port;
        this.threadNum = threadNum;

        //实例化一些基本变量
        this.threads = ThreadManager.forkJoinPoolThreadManager();
//        this.threads = new ThreadManager(
//                new ThreadPoolExecutor(this.threadNum, this.threadNum, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>()));
        JvmCloseCleaner.DEFAULT().add(this::shutdownNow);
    }

    /**
     * 支持动态添加服务
     */
    public void addService(Object service, Class<?> interfaceClass) {
        JavaProviderInvoker invoker = new JavaProviderInvoker(service, interfaceClass);
        String realServiceName = invoker.getServiceName();

        synchronized (serviceMap) {
            if (!serviceMap.containsKey(realServiceName)) {
                serviceMap.put(invoker.getServiceName(), invoker);
            } else {
                throw new IllegalStateException("service'" + realServiceName + "' has registered. can not register again");
            }
        }
    }

    /**
     * 支持动态移除服务
     */
    public void disableService(String serviceName) {
        synchronized (serviceMap) {
            serviceMap.remove(serviceName);
        }
    }

    /**
     * 启动Server
     */
    public void start() {
        log.info("server(port= " + port + ") starting...");
        //启动连接
        this.connection = new ProviderConnection(new InetSocketAddress("localhost", this.port), this);
        try {
            connection.bind();
        } catch (Exception e) {
            log.error("", e);
            System.exit(-1);
        }
        log.info("server connection close successfully");

        //启动定时扫描队列,以及时处理所有consumer的请求
        this.scanRequestsThread = new ScanRequestsThread();
        this.scanRequestsThread.start();

        log.info("server(port= " + port + ") started");
    }

    /**
     * 默认每个服务关闭都需要关闭Server
     * 但如果仍然有服务在此Server上提供服务,则仍然运行该Server
     */
    public void shutdown() {
        log.info("server(port= " + port + ") shutdowning...");
        synchronized (serviceMap) {
            int refCounter = serviceMap.size();
            if (refCounter > 0) {
                log.info("server still has service to server, don't stop");
                return;
            }
        }

        shutdownNow();
    }

    /**
     * 不管3721,马上stop
     */
    public void shutdownNow() {
        if (this.connection == null || scanRequestsThread == null) {
            log.error("Server has not started call shutdown");
            throw new IllegalStateException("Provider Server has not started");
        }

        log.info("server shutdown now(some resource may be still running)");
        //关闭扫描请求队列线程  停止将队列中的请求的放入线程池中处理,转而发送重试的RPCResponse
        scanRequestsThread.setStopped(true);
        //中断对requestsQueue的take()阻塞操作
        scanRequestsThread.interrupt();
        try {
            Thread.sleep(500L);
        } catch (InterruptedException e) {
            ExceptionUtils.log(e);
        }
        //处理完所有进入队列的请求
        threads.shutdown();
        log.info("thread pool shutdown successfully");
        try {
            Thread.sleep(200L);
        } catch (InterruptedException e) {
            ExceptionUtils.log(e);
        }

        //最后关闭连接
        connection.close();
        log.info("connection stop successfully");
        log.info("server stop successfully");

        isStopped = true;
    }

    @Override
    public void handleRequest(RPCRequest rpcRequest) {
        singleThread.execute(() -> {
            try {
                requestsQueue.put(rpcRequest);
            } catch (InterruptedException e) {
                ExceptionUtils.log(e);
            }
        });
    }

    private class ScanRequestsThread extends Thread {
        private final Logger log = LoggerFactory.getLogger("invoker");
        private boolean stopped = false;

        public void run() {
            log.info("request scanner thread started");
            log.info("ready to handle consumer's request");
            while (!stopped) {
                try {
                    final RPCRequest rpcRequest = requestsQueue.take();
                    log.info("收到一个请求");
                    //提交线程池处理服务执行
                    threads.submit(() -> {
                        String serviceName = rpcRequest.getServiceName();
                        String methodName = rpcRequest.getMethod();
                        Object[] params = rpcRequest.getParams();
                        Channel channel = rpcRequest.getChannel();

                        ProviderInvoker invoker = serviceMap.get(serviceName);

                        RPCResponse rpcResponse = new RPCResponse(rpcRequest.getRequestId(), rpcRequest.getServiceName(), rpcRequest.getMethod());
                        Object result = null;
                        if (invoker != null) {
                            try {
                                result = invoker.invoke(methodName, params);
                            } catch (Throwable throwable) {
                                //服务调用报错, 将异常信息返回
                                rpcResponse.setState(RPCResponse.State.ERROR, throwable.getMessage());
                                rpcResponse.setResult(null);
                                channel.writeAndFlush(rpcResponse);
                                log.error("", throwable);
                            }
                            rpcResponse.setState(RPCResponse.State.SUCCESS, "");
                        } else {
                            log.error("can not find rpcRequest(id= " + rpcRequest.getRequestId() + ")'s service");
                            rpcResponse.setState(RPCResponse.State.ERROR, "can not find service '" + serviceName + "'");
                        }

                        rpcResponse.setResult(result);
                        //写回给消费者
                        channel.writeAndFlush(rpcResponse);
                    });

                } catch (InterruptedException e) {
                    ExceptionUtils.log(e);
                }
            }
            log.info("request scanner thread state change");
            log.info("ready to response directly and ask client to retry service call");


            for (RPCRequest rpcRequest : requestsQueue) {
                //创建RPCResponse,设置服务不可用请求重试标识,直接回发
                Channel channel = rpcRequest.getChannel();

                RPCResponse rpcResponse = new RPCResponse(rpcRequest.getRequestId(), rpcRequest.getServiceName(), rpcRequest.getMethod());
                rpcResponse.setState(RPCResponse.State.RETRY, "server unavailable");

                channel.write(rpcResponse);
            }
            requestsQueue.clear();
            log.info("request scanner thread stop");

        }

        public void setStopped(boolean stopped) {
            this.stopped = stopped;
        }
    }

}
