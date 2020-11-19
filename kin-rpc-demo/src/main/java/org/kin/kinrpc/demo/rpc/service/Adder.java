package org.kin.kinrpc.demo.rpc.service;

import org.kin.kinrpc.rpc.ProviderFutureContext;
import org.kin.kinrpc.rpc.RpcThreadPool;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by 健勤 on 2017/2/16.
 */
public class Adder implements Addable {
    @Override
    public int add(int a, int b) {
        return a + b;
    }

    @Override
    public void print(String content) {
        System.out.println(content);
    }

    @Override
    public int get(int a) {
        return a;
    }

    @Override
    public String get(String a) {
        return a;
    }

    @Override
    public void throwException() {
        throw new RuntimeException("test throw Exception");
    }

    @Override
    public Return1 notifyTest() {
        return new Return1();
    }

    @Override
    public Return2 asyncReturn() {
        ScheduledFuture<Return2> future = RpcThreadPool.executors().schedule(Return2::new, 1, TimeUnit.SECONDS);
        ProviderFutureContext.updateFuture(future);
        return null;
    }
}
