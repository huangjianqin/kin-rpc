package org.kin.kinrpc.demo.service;

/**
 * Created by 健勤 on 2017/2/16.
 */
public class Adder implements Addable {
    @Override
    public int add(int a, int b) {
        return a + b;
    }
}
