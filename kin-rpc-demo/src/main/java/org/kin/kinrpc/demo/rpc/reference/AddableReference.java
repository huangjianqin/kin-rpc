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
    protected static ReferenceConfig<Addable> config() {
        return References.reference(Addable.class)
                .async()
                .notify(Return1Notifier.N)
                .rate(10000);
    }

    protected static void roundTest(ReferenceConfig<Addable> config) throws Exception {
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
            } catch (Exception e) {
                System.err.println(e);
            }

            TimeUnit.MILLISECONDS.sleep(300);
        }
        service.print(++count + "");
        System.out.println("结束");
        config.disable();
        System.exit(0);
    }
}
