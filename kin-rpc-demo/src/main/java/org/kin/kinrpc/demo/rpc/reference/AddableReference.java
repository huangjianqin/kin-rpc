package org.kin.kinrpc.demo.rpc.reference;

import org.kin.kinrpc.cluster.RpcContext;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.References;
import org.kin.kinrpc.demo.rpc.service.Addable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2020/11/16
 */
public class AddableReference {
    public static ReferenceConfig<Addable> config() {
        return References.reference(Addable.class)
                .version("001")
                .async()
                .notify(Return1Notifier.N, Return2Notifier.N)
                .callTimeout(2000)
                .rate(10000);
    }

    public static void roundTest(ReferenceConfig<Addable> config) throws Exception {
        Addable service = config.get();
        int count = 0;
        while (count < 10000) {
            try {
                service.add(1, 1);
                CompletableFuture<Object> future = RpcContext.future();
                System.out.println("结果" + future.get());

                service.print(++count + "");
                service.get(1);
                future = RpcContext.future();
                System.out.println(future.get());
                service.notifyTest();

                service.asyncReturn();
            } catch (Exception e) {
                e.printStackTrace();
            }

            TimeUnit.MILLISECONDS.sleep(300);
        }
        service.print(++count + "");
        System.out.println("结束");
        config.disable();
        System.exit(0);
    }
}
