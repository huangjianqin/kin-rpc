package org.kin.kinrpc.remoting.transport;

import io.netty.channel.ChannelHandlerContext;
import org.apache.log4j.Logger;
import org.kin.kinrpc.common.Constants;
import org.kin.kinrpc.config.ServerConfig;
import org.kin.kinrpc.remoting.transport.bootstrap.Connection;
import org.kin.kinrpc.remoting.transport.bootstrap.ProviderConnection;
import org.kin.kinrpc.rpc.invoker.JavaProviderInvoker;
import org.kin.kinrpc.rpc.invoker.ProviderInvoker;
import org.kin.kinrpc.rpc.protocol.RPCRequest;
import org.kin.kinrpc.rpc.protocol.RPCResponse;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by 健勤 on 2017/2/10.
 * 可以作为多个服务的Server
 */
public class Server {
    private static final Logger log = Logger.getLogger(Server.class);

    //只有get的时候没有同步,其余都同步了
    //大部分情况下get调用的频率比其他方法都多,没有必要使用同步容器,提供一丢丢性能
    private final Map<String, ProviderInvoker> serviceMap = new HashMap<String, ProviderInvoker>();
    //各种服务请求处理的线程池
    private final ThreadPoolExecutor threads;
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

    public Server(ServerConfig serverConfig) {
        this.port = serverConfig.getPort();
        this.threadNum = serverConfig.getThreadNum();

        //实例化一些基本变量
        this.threads = new ThreadPoolExecutor(this.threadNum, this.threadNum, Constants.SERVER_DEFAULT_THREADS_ALIVE, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
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
        this.connection = new ProviderConnection(new InetSocketAddress("localhost", this.port), requestsQueue);
        new Thread(new Runnable() {
            public void run() {
                connection.bind();
                log.info("server connection close successfully");
            }
        }).start();

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
        //停止将队列中的请求的放入线程池中处理,转而发送重试的RPCResponse
        scanRequestsThread.setServerStopped(true);
        //中断对requestsQueue的take()阻塞操作
        scanRequestsThread.interrupt();
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //处理完所有进入队列的请求
        threads.shutdown();
        log.info("thread pool shutdown successfully");
        //关闭扫描请求队列线程
        scanRequestsThread.setStopped(true);
        //中断对requestsQueue的take()阻塞操作
        scanRequestsThread.interrupt();
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //最后关闭连接
        connection.close();
        log.info("connection stop successfully");
        log.info("server stop successfully");

        isStopped = true;
    }

    class ScanRequestsThread extends Thread {
        private final Logger log = Logger.getLogger(ScanRequestsThread.class);
        private boolean serverStopped = false;
        private boolean stopped = false;

        public void run() {
            log.info("request scanner thread started");
            log.info("ready to handle consumer's request");
            while (!serverStopped) {
                try {
                    final RPCRequest rpcRequest = requestsQueue.take();
                    log.info("收到一个请求");
                    //提交线程池处理服务执行
                    threads.submit(new Runnable() {
                        public void run() {
                            String serviceName = rpcRequest.getServiceName();
                            String methodName = rpcRequest.getMethod();
                            Object[] params = rpcRequest.getParams();
                            ChannelHandlerContext writeBack = rpcRequest.getCtx();

                            ProviderInvoker invoker = serviceMap.get(serviceName);

                            RPCResponse rpcResponse = new RPCResponse(rpcRequest.getRequestId());
                            Object result = null;
                            if (invoker != null) {
                                result = invoker.invoke(methodName, params);
                                rpcResponse.setState(RPCResponse.State.SUCCESS, "");
                            } else {
                                log.error("can not find rpcRequest(id= " + rpcRequest.getRequestId() + ")'s service");
                                rpcResponse.setState(RPCResponse.State.ERROR, "can not find service '" + serviceName + "'");
                            }

                            rpcResponse.setResult(result);
                            //写回给消费者
                            writeBack.writeAndFlush(rpcResponse);

                        }
                    });

                } catch (InterruptedException e) {
                    log.info("submit service execute interrupted");
                    //继续运行?
//                    run();
                }
            }
            log.info("request scanner thread state change");
            log.info("ready to response directly and ask client to retry service call");

            while (!stopped) {
                try {
                    RPCRequest rpcRequest = requestsQueue.take();
                    //创建RPCResponse,设置服务不可用请求重试标识,直接回发
                    ChannelHandlerContext writeBack = rpcRequest.getCtx();

                    RPCResponse rpcResponse = new RPCResponse(rpcRequest.getRequestId());
                    rpcResponse.setState(RPCResponse.State.RETRY, "server unavailable");

                    writeBack.write(rpcResponse);

                } catch (InterruptedException e) {
                    log.info("send unavailable service execute interrupted");
//                    Thread.currentThread().interrupt();
                }
            }
            log.info("request scanner thread stop");

        }

        public void setServerStopped(boolean serverStopped) {
            this.serverStopped = serverStopped;
        }

        public void setStopped(boolean stopped) {
            this.stopped = stopped;
        }
    }

    /**
     * 动态设置线程池的最大线程数,线程空闲后会被删除到指定数量之下
     *
     * @param threadNum
     */
    public void setMaxThreadsNum(int threadNum) {
        this.threadNum = threadNum;
        this.threads.setMaximumPoolSize(this.threadNum);
    }

}
