package org.kin.kinrpc.demo.service;

/**
 * Created by 健勤 on 2017/2/16.
 */
public interface Addable {
    int add(int a, int b);

    void print(String content);

    int get(int a);

    String get(String a);

    void throwException();
}
