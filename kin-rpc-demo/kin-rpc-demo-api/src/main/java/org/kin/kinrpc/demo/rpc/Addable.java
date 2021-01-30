package org.kin.kinrpc.demo.rpc;

import java.util.concurrent.Future;

/**
 * Created by 健勤 on 2017/2/16.
 */
//@KinRpcReference(urls = "kinrpc://0.0.0.0:16888", async = true)
public interface Addable {
    int add(int a, int b);

    void print(String content);

    int get(int a);

    String get(String a);

    void throwException();

    Return1 notifyTest();

    Return2 asyncReturn();

    Future<Return3> returnFuture();
}
