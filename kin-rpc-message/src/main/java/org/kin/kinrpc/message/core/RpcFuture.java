package org.kin.kinrpc.message.core;

import org.kin.framework.concurrent.lock.OneLock;

import java.io.Serializable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author huangjianqin
 * @date 2020-06-14
 */
public class RpcFuture<R extends Serializable> implements Future<R> {
    private OneLock sync = new OneLock();
    private R reply;


    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public R get() throws InterruptedException, ExecutionException {
        return null;
    }

    @Override
    public R get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }

    public void done() {
        sync.release(1);
    }
}
