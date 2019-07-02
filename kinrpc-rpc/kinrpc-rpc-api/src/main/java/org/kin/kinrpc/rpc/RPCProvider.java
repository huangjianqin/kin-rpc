package org.kin.kinrpc.rpc;

import io.netty.channel.Channel;
import org.kin.framework.concurrent.SimpleThreadFactory;
import org.kin.framework.concurrent.ThreadManager;
import org.kin.kinrpc.common.URL;
import org.kin.kinrpc.rpc.exception.RateLimitException;
import org.kin.kinrpc.rpc.invoker.AbstractProviderInvoker;
import org.kin.kinrpc.rpc.invoker.impl.ProviderInvokerImpl;
import org.kin.kinrpc.rpc.serializer.Serializer;
import org.kin.kinrpc.rpc.transport.ProviderHandler;
import org.kin.kinrpc.rpc.transport.common.RPCConstants;
import org.kin.kinrpc.rpc.transport.domain.RPCRequest;
import org.kin.kinrpc.rpc.transport.domain.RPCResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Created by 健勤 on 2017/2/10.
 * 可以作为多个服务的Server
 */
public class RPCProvider {
    private static final Logger log = LoggerFactory.getLogger("invoker");

    //只有get的时候没有同步,其余都同步了
    //大部分情况下get调用的频率比其他方法都多,没有必要使用同步容器,提供一丢丢性能
    private Map<String, ProviderInvokerWrapper> serviceMap = new ConcurrentHashMap<>();
    //各种服务请求处理的线程池
    private ForkJoinPool threads;
    //保证RPCRequest按请求顺序进队
    private ThreadManager orderQueueRequestsThread = new ThreadManager(
            new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(), new SimpleThreadFactory("order-queue-requests")));
    //RPCRequest队列,所有连接该Server的reference发送的request都put进这个队列
    //然后由一个专门的线程不断地get,再提交到线程池去处理
    //本质上是生产者-消费者模式
    private BlockingQueue<RPCRequest> requestsQueue = new LinkedBlockingQueue<>();

    //占用端口
    private int port;
    //序列化方式
    private Serializer serializer;
    //底层的连接
    private ProviderHandler connection;
    //扫描RPCRequest的线程
    private ScanRequestsThread scanRequestsThread;
    //标识是否stopped
    private volatile boolean isStopped = false;

    public RPCProvider(int port, Serializer serializer) {
        this.port = port;
        this.serializer = serializer;

        this.threads = new ForkJoinPool();
    }

    /**
     * 支持动态添加服务
     */
    public void addService(URL url, Class<?> interfaceClass, Object service) {
        if(!isStopped){
            String serviceName = url.getServiceName();
            ProviderInvokerImpl invoker = new ProviderInvokerImpl(serviceName, service);
            invoker.init(interfaceClass);

            if (!serviceMap.containsKey(serviceName)) {
                serviceMap.put(serviceName, new ProviderInvokerWrapper(url, invoker));
            } else {
                throw new IllegalStateException("service'" + serviceName + "' has registered. can not register again");
            }
        }
    }

    /**
     * 支持动态移除服务
     */
    public void disableService(URL url) {
        String serviceName = url.getServiceName();
        serviceMap.remove(serviceName);
    }

    public boolean isBusy(){
        if(isAlive()){
            return !serviceMap.isEmpty();
        }

        return false;
    }

    public boolean isAlive(){
        return connection.isActive();
    }

    public Collection<URL> getAvailableServices(){
        List<ProviderInvokerWrapper> copy = new ArrayList<>(serviceMap.values());
        return copy.stream().map(ProviderInvokerWrapper::getUrl).collect(Collectors.toList());
    }

    public int getPort() {
        return port;
    }

    /**
     * 启动Server
     */
    public void start() {
        if(isStopped){
            throw new RuntimeException("try start stopped provider");
        }
        log.info("provider(port={}) starting...", port);
        //启动连接
        this.connection = new ProviderHandler(new InetSocketAddress(this.port), this, serializer);
        try {
            connection.bind();
        } catch (Exception e) {
            log.error("", e);
            System.exit(-1);
        }

        //启动定时扫描队列,以及时处理所有reference的请求
        this.scanRequestsThread = new ScanRequestsThread();
        this.scanRequestsThread.start();

        log.info("provider(port={}) started", port);
    }

    /**
     * 默认每个服务关闭都需要关闭Server
     * 但如果仍然有服务在此Server上提供服务,则仍然运行该Server
     */
    public void shutdown() {
        if(isStopped){
            throw new RuntimeException("try shutdown stopped provider");
        }
        shutdownNow();
    }

    /**
     * 不管3721,马上stop
     */
    public void shutdownNow() {
        if(isStopped){
            return;
        }
        if (this.connection == null || scanRequestsThread == null) {
            throw new IllegalStateException("Provider Server has not started");
        }
        log.info("server(port= " + port + ") shutdowning...");
        isStopped = true;

        //关闭扫描请求队列线程  停止将队列中的请求的放入线程池中处理,转而发送重试的RPCResponse
        scanRequestsThread.stopScan();
        //中断对requestsQueue的take()阻塞操作
        scanRequestsThread.interrupt();
        //处理完所有进入队列的请求
        threads.shutdown();
        try {
            //等待所有response成功返回
            Thread.sleep(500L);
        } catch (InterruptedException e) {
            log.error("", e);
        }

        //最后关闭连接
        connection.close();
        log.info("server connection close successfully");
    }

    public void handleRequest(RPCRequest rpcRequest) {
        if(!isStopped){
            orderQueueRequestsThread.execute(() -> {
                try {
                    requestsQueue.put(rpcRequest);
                } catch (InterruptedException e) {
                    log.error("", e);
                }
            });
        }
    }

    private class ScanRequestsThread extends Thread {
        private final Logger log = LoggerFactory.getLogger("invoker");
        private volatile boolean isStopped = false;

        public ScanRequestsThread() {
            super("requests-scanner--thread-1");
        }

        @Override
        public void run() {
            log.info("handling reference's request...");
            while (!isStopped) {
                try {
                    final RPCRequest rpcRequest = requestsQueue.take();
                    log.debug("revceive a request >>> " + System.lineSeparator() + rpcRequest);

                    //因为fork-join的工作窃取机制, 会优先窃取队列靠后的task(maybe后面才来request)
                    //因此, 限制队列的任务数, 以此做到尽可能先完成早到的request
                    long queuedTaskCount = threads.getQueuedTaskCount();
                    while(queuedTaskCount > RPCConstants.POOL_TASK_NUM){
                        log.warn("too many task(num={}) to execute, slow down", queuedTaskCount);
                        Thread.sleep(200);
                    }

                    //提交线程池处理服务执行
                    threads.execute(() -> {
                        rpcRequest.setHandleTime(System.currentTimeMillis());
                        String serviceName = rpcRequest.getServiceName();
                        String methodName = rpcRequest.getMethod();
                        Object[] params = rpcRequest.getParams();
                        Channel channel = rpcRequest.getChannel();

                        RPCResponse rpcResponse = new RPCResponse(rpcRequest.getRequestId(),
                                rpcRequest.getServiceName(), rpcRequest.getMethod());
                        if(serviceMap.containsKey(serviceName)){
                            AbstractProviderInvoker invoker = serviceMap.get(serviceName).getInvoker();

                            Object result = null;
                            try {
                                result = invoker.invoke(methodName, false, params);
                                rpcResponse.setState(RPCResponse.State.SUCCESS, "success");
                            } catch (RateLimitException e) {
                                rpcResponse.setState(RPCResponse.State.RETRY, "service rate limited, just reject");
                            } catch (Throwable throwable) {
                                //服务调用报错, 将异常信息返回
                                rpcResponse.setState(RPCResponse.State.ERROR, throwable.getMessage());
                                log.error("", throwable);
                            }

                            rpcResponse.setResult(result);
                        }
                        else{
                            log.error("can not find service>>> {}", rpcRequest);
                            rpcResponse.setState(RPCResponse.State.ERROR, "unknown service");
                        }
                        rpcResponse.setCreateTime(System.currentTimeMillis());
                        //write back to reference
                        connection.resp(channel, rpcResponse);
                    });
                } catch (InterruptedException e) {

                }
            }
            log.info("response directly and ask all requests to retry");

            for (RPCRequest rpcRequest : requestsQueue) {
                //创建RPCResponse,设置服务不可用请求重试标识,直接回发
                Channel channel = rpcRequest.getChannel();

                RPCResponse rpcResponse = new RPCResponse(rpcRequest.getRequestId(), rpcRequest.getServiceName(), rpcRequest.getMethod());
                rpcResponse.setState(RPCResponse.State.RETRY, "service unavailable");

                channel.write(rpcResponse);
            }
            requestsQueue.clear();
            log.info("requests scanner thread stop");

        }

        public void stopScan() {
            this.isStopped = true;
        }
    }

    private class ProviderInvokerWrapper {
        private URL url;
        private AbstractProviderInvoker invoker;

        public ProviderInvokerWrapper(URL url, AbstractProviderInvoker invoker) {
            this.url = url;
            this.invoker = invoker;
        }

        //getter
        public URL getUrl() {
            return url;
        }

        public AbstractProviderInvoker getInvoker() {
            return invoker;
        }
    }
}
