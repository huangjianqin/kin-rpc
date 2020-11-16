package org.kin.kinrpc.demo.rpc.reference;

import org.kin.kinrpc.cluster.RpcContext;
import org.kin.kinrpc.cluster.exception.CannotFindInvokerException;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.References;
import org.kin.kinrpc.demo.rpc.service.Addable;
import org.kin.kinrpc.rpc.exception.RpcCallErrorException;
import org.kin.kinrpc.rpc.exception.RpcRetryException;
import org.kin.kinrpc.rpc.exception.UnknownRpcResponseStateCodeException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2019/7/3
 */
public class KinRpcZookeeperAddableReference {
    public static void main(String[] args) throws InterruptedException {
        ReferenceConfig<Addable> referenceConfig =
                References.reference(Addable.class)
                        .async()
                        .zookeeper("127.0.0.1:2181")
                        .registrySessionTimeout(1000);

        Addable service = referenceConfig.get();
        int count = 0;
        while (true) {
            try {
//                Object result = service.add(1, 1);
//                System.out.println("结果" + result);
//                service.print(++count + "");
                service.get(1);
                CompletableFuture<Object> future = RpcContext.future();
                System.out.println(future.get());
//                CompletableFuture<String> completableFuture = service.get("A");
//                System.out.println(completableFuture.handleAsync((s, t) -> s + s).get());
//                service.throwException();
            } catch (RpcRetryException | CannotFindInvokerException | RpcCallErrorException | UnknownRpcResponseStateCodeException | ExecutionException e) {

            }

            TimeUnit.SECONDS.sleep(5);
        }
    }
}
