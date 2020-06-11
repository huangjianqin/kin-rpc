package org.kin.kinrpc.demo.rpc.reference;

import org.kin.kinrpc.cluster.exception.CannotFindInvokerException;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.References;
import org.kin.kinrpc.rpc.exception.RpcCallErrorException;
import org.kin.kinrpc.rpc.exception.RpcRetryException;
import org.kin.kinrpc.rpc.exception.UnknownRpcResponseStateCodeException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2019/7/2
 */
public class ZookeeperAddableReference {
    public static void main(String[] args) throws InterruptedException {
        ReferenceConfig<AddableReference.Addable> referenceConfig =
                References.reference(AddableReference.Addable.class)
                        .serviceName("test/Add")
                        .zookeeper("127.0.0.1:2181").registrySessionTimeout(60 * 1000);

        AddableReference.Addable service = referenceConfig.get();
        int count = 0;
        while (true) {
            try {
//                Object result = service.add(1, 1);
//                System.out.println("结果" + result);
//                service.print(++count + "");
                Future future = service.get(1);
                System.out.println(future.get());
//                CompletableFuture<String> completableFuture = service.get("A");
//                System.out.println(completableFuture.handleAsync((s, t) -> s + s).get());
//                service.throwException();
            } catch (RpcRetryException | CannotFindInvokerException | RpcCallErrorException | UnknownRpcResponseStateCodeException | ExecutionException e) {

            }

            TimeUnit.SECONDS.sleep(5);
        }
    }

    public interface Addable {
        int add(int a, int b);

        void print(String content);

        <T> Future<T> get(int a);

        <T> CompletableFuture<T> get(String a);

        void throwException();
    }
}
