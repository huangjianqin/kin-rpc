package org.kin.kinrpc.rpc.demo.service;

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
}
