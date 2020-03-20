package org.kin.kinrpc.demo.reference;

import org.kin.framework.utils.NetUtils;
import org.kin.kinrpc.cluster.exception.CannotFindInvokerException;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.References;
import org.kin.kinrpc.config.SerializerType;
import org.kin.kinrpc.rpc.exception.RPCCallErrorException;
import org.kin.kinrpc.rpc.exception.RPCRetryException;
import org.kin.kinrpc.rpc.exception.UnknownRPCResponseStateCodeException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2019/7/1
 */
public class AddableReference {
    public static void main(String[] args) throws InterruptedException {
        ReferenceConfig<Addable> referenceConfig =
                References.reference(Addable.class)
                        .serviceName("org.kin.kinrpc.demo.service.Addable").urls(NetUtils.getIpPort(16888))
                        .serialize(SerializerType.JSON.getType())
                        .version("001")
                        .compress();

        Addable service = referenceConfig.get();
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
            } catch (RPCRetryException | CannotFindInvokerException | RPCCallErrorException | UnknownRPCResponseStateCodeException | ExecutionException e) {

            }

            TimeUnit.MILLISECONDS.sleep(300);
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
