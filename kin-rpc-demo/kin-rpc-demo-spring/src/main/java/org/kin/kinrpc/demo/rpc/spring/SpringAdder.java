package org.kin.kinrpc.demo.rpc.spring;

import org.kin.kinrpc.demo.rpc.*;
import org.kin.kinrpc.spring.KinRpcService;

import java.util.concurrent.Future;

/**
 * @author huangjianqin
 * @date 2020/12/15
 */
@KinRpcService(Addable.class)
public class SpringAdder implements Addable {
    private final Addable addable = new Adder();

    @Override
    public int add(int a, int b) {
        return addable.add(a, b);
    }

    @Override
    public void print(String content) {
        addable.print(content);
    }

    @Override
    public int get(int a) {
        return addable.get(a);
    }

    @Override
    public String get(String a) {
        return addable.get(a);
    }

    @Override
    public void throwException() {
        addable.throwException();
    }

    @Override
    public Return1 notifyTest() {
        return addable.notifyTest();
    }

    @Override
    public Return2 asyncReturn() {
        return addable.asyncReturn();
    }

    @Override
    public Future<Return3> returnFuture() {
        return addable.returnFuture();
    }
}