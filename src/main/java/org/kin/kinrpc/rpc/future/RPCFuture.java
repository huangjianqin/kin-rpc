package org.kin.kinrpc.rpc.future;

import org.apache.log4j.Logger;
import org.kin.kinrpc.rpc.protocol.RPCRequest;
import org.kin.kinrpc.rpc.protocol.RPCResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by 健勤 on 2017/2/15.
 */
public class RPCFuture implements Future<Object> {
    private static final Logger log = Logger.getLogger(RPCFuture.class);

    private Sync sync;
    private RPCRequest request;
    private RPCResponse response;

    //所有RPCFuture实例共用一个线程池
    private static final ThreadPoolExecutor threads = new ThreadPoolExecutor(2, 16, 600L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    //添加JVM关闭钩子,以确保释放该静态线程池
    static{
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                threads.shutdown();
            }
        }));
    }

    private List<AsyncRPCCallback> callbacks = new ArrayList<AsyncRPCCallback>();
    private ReentrantLock lock = new ReentrantLock();

    //用于记录服务调用的耗时(毫秒),衡量负载
    private long startTime;
    private long responseTimeThreshold = 5000;

    public RPCFuture(RPCRequest request) {
        this.sync = new Sync();
        this.request = request;
        this.startTime = System.currentTimeMillis();
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    public boolean isCancelled() {
        throw new UnsupportedOperationException();
    }

    public boolean isDone() {
        return sync.isDone();
    }

    public Object get() throws InterruptedException, ExecutionException {
        sync.acquire(-1);
        if(isDone()){
            return this.response.getResult();
        }
        return null;
    }

    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        boolean success = sync.tryAcquireNanos(-1, unit.toNanos(timeout));
        if(success){
            if(isDone()){
                return this.response.getResult();
            }
            else{
                return null;
            }
        }
        else{
            throw new RuntimeException("Timeout exception. Request id: " + this.request.getRequestId()
                    + ". Request class name: " + this.request.getServiceName()
                    + ". Request method: " + this.request.getMethod());
        }
    }

    public void done(RPCResponse response){
        sync.release(1);
        this.response = response;
        invokeAllCallBacks();

        long responseTime = System.currentTimeMillis() - startTime;
        if (responseTime > this.responseTimeThreshold) {
            log.info("Service response time is too slow. Request id = " + response.getRequestId() + ". Response Time = " + responseTime + "ms");
        }
    }

    public RPCFuture addRPCCallback(AsyncRPCCallback callback){
        lock.lock();
        try{
            if(this.isDone()){
                runCallBack(callback);
            }
            else{
                this.callbacks.add(callback);
            }
        }finally {
            lock.unlock();
        }

        return this;
    }

    private void runCallBack(final AsyncRPCCallback callback){
        final RPCResponse response = this.response;
        threads.submit(new Runnable() {
            public void run() {
                if(!response.getState().equals(RPCResponse.State.ERROR)){
                    callback.success(response.getResult());
                }
                else{
                    callback.fail(new RuntimeException("Response error", new Throwable(response.getInfo())));
                }
            }
        });
    }

    private void invokeAllCallBacks(){
        lock.lock();

        try{
            for(AsyncRPCCallback callback: this.callbacks){
                runCallBack(callback);
            }
        }
        finally {
            lock.unlock();
        }
    }

    class Sync extends AbstractQueuedSynchronizer{
        private final int DONE = 1;
        private final int PENDING = 0;

        @Override
        protected boolean tryAcquire(int acquires) {
            return getState() == DONE ? true : false;
        }

        @Override
        protected boolean tryRelease(int releases) {
            if(getState() == PENDING){
                if(compareAndSetState(PENDING, DONE)){
                    return true;
                }
            }

            return false;
        }

        public boolean isDone(){
            return getState() == DONE;
        }
    }
}
