package org.kin.kinrpc.demo.rpc.spring;

import org.kin.kinrpc.demo.rpc.Addable;
import org.kin.kinrpc.demo.rpc.Return1;
import org.kin.kinrpc.demo.rpc.Return2;
import org.kin.kinrpc.rpc.ProviderFutureContext;
import org.kin.kinrpc.rpc.RpcThreadPool;
import org.kin.kinrpc.spring.KinRpcService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2020/12/15
 */
@SpringBootApplication
@KinRpcService(interfaceClass = Addable.class)
public class SpringAdder implements Addable {
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

    public static void main(String[] args) {
        SpringApplication.run(SpringAdder.class);
    }
}