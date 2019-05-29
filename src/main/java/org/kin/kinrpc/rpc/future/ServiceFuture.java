package org.kin.kinrpc.rpc.future;

import org.apache.log4j.Logger;
import org.kin.kinrpc.config.ServiceConfig;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by 健勤 on 2017/2/13.
 */
public class ServiceFuture implements Future<ServiceConfig> {
    private static final Logger log = Logger.getLogger(ServiceFuture.class);

    private ServiceConfig serviceConfig;

    public ServiceFuture(ServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    public boolean isCancelled() {
        throw new UnsupportedOperationException();
    }

    public boolean isDone() {
        return true;
    }

    public ServiceConfig get() throws InterruptedException, ExecutionException {
        return this.serviceConfig;
    }

    public ServiceConfig get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return get();
    }

    public void sync(){
        synchronized (serviceConfig){
            try {
                log.info("waiting service disable...");
                this.serviceConfig.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
                //恢复中断标识
//            Thread.currentThread().interrupt();
            }
        }
    }

    public void sync(long timeout){
        synchronized (serviceConfig){
            try {
                log.info("waiting service disable for (" + timeout + "ms)...");
                this.serviceConfig.wait(timeout);
            } catch (InterruptedException e) {
                e.printStackTrace();
                //恢复中断标识
//            Thread.currentThread().interrupt();
            }
        }
    }
}
