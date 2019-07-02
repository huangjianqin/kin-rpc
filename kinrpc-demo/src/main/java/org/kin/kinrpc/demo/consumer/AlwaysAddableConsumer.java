package org.kin.kinrpc.demo.consumer;

import exception.CannotFindInvokerException;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.References;
import org.kin.kinrpc.rpc.cluster.Clusters;
import org.kin.kinrpc.rpc.exception.RPCCallErrorException;
import org.kin.kinrpc.rpc.exception.RPCRetryException;
import org.kin.kinrpc.rpc.exception.UnknownRPCResponseStateCodeException;
import org.kin.kinrpc.transport.statistic.InOutBoundStatisicService;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author huangjianqin
 * @date 2019/7/1
 */
public class AlwaysAddableConsumer {
    public static void main(String[] args) throws InterruptedException {
        ReferenceConfig<Addable> referenceConfig =
                References.reference(Addable.class).serviceName("org/kin/kinrpc/demo/service/Addable").urls("0.0.0.0:16888");

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
            } catch (RPCRetryException e) {
                System.err.println(e);
            } catch (CannotFindInvokerException e) {
                System.err.println(e);
            } catch (RPCCallErrorException e) {
                System.err.println(e);
            } catch (UnknownRPCResponseStateCodeException e) {
                System.err.println(e);
            } catch (ExecutionException e) {
                System.err.println(e);
            }

            Thread.sleep(5000);
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
