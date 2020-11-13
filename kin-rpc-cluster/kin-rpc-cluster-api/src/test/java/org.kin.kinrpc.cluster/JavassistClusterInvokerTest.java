package org.kin.kinrpc.cluster;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * @author huangjianqin
 * @date 2019-09-09
 */
public class JavassistClusterInvokerTest {
    public static void main(String[] args) {
        JavassistClusterInvoker<Addable> jci = new JavassistClusterInvoker<>(null, null,
                Addable.class, Collections.emptyList());
        Addable proxy = jci.proxy();
        System.out.println(proxy);
    }

    public interface Addable {
        int add(int a, int b);

        void print(String content);

        <T> Future<T> get(int a);

        <T> CompletableFuture<T> get(String a);

        void throwException();
    }

}
