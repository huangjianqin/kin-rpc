package org.kin.kinrpc.demo.rpc.reference;

import org.kin.kinrpc.cluster.RpcContext;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.References;
import org.kin.kinrpc.demo.rpc.service.Addable;
import org.kin.kinrpc.rpc.common.Constants;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2019/7/1
 */
public class AddableReference {
    public static void main(String[] args) throws InterruptedException {
        ReferenceConfig<Addable> referenceConfig =
                References.reference(Addable.class)
                        .urls("kinrpc://0.0.0.0:16888?"
                                .concat(Constants.SERVICE_NAME_KEY).concat("=").concat(Addable.class.getName())
                                .concat(Constants.VERSION_KEY).concat("=").concat("001"))
                        .async()
                        .rate(10000);

        Addable service = referenceConfig.get();
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
            } catch (Exception e) {
                System.err.println(e);
            }

            TimeUnit.MILLISECONDS.sleep(300);
        }
        service.print(++count + "");
        System.out.println("结束");
        referenceConfig.disable();
        System.exit(0);
    }
}
