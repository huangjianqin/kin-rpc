package org.kin.kinrpc.demo.rpc.kinrpc;

import org.kin.kinrpc.cluster.RpcCallContext;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.References;
import org.kin.kinrpc.config.ZookeeperRegistryConfig;
import org.kin.kinrpc.demo.rpc.Addable;

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
                        .registry(ZookeeperRegistryConfig.create("127.0.0.1:2181").sessionTimeout(1000).build());

        Addable service = referenceConfig.get();
        int count = 0;
        while (true) {
            try {
//                Object result = service.add(1, 1);
//                System.out.println("结果" + result);
//                service.print(++count + "");
                service.get(1);
                CompletableFuture<Object> future = RpcCallContext.future();
                System.out.println(future.get());
//                CompletableFuture<String> completableFuture = service.get("A");
//                System.out.println(completableFuture.handleAsync((s, t) -> s + s).get());
//                service.throwException();
            } catch (ExecutionException e) {

            }

            TimeUnit.SECONDS.sleep(5);
        }
    }
}
