package org.kin.kinrpc.demo.rpc.spring;

import org.kin.kinrpc.cluster.RpcContext;
import org.kin.kinrpc.demo.rpc.Addable;
import org.kin.kinrpc.spring.KinRpcReference;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2020/12/15
 */
@Component
public class SpringAdderReference {
    @KinRpcReference(urls = "kinrpc://0.0.0.0:16888", async = true)
    private Addable addable;

    @PostConstruct
    public void test() {
        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                //ignore
            }
            int count = 0;
            while (count < 10000) {
                try {
                    addable.add(1, 1);
                    CompletableFuture<Object> future = RpcContext.future();
                    System.out.println("结果" + future.get());

                    addable.print(++count + "");
                    addable.get(1);
                    future = RpcContext.future();
                    System.out.println(future.get());
                    addable.notifyTest();

                    addable.asyncReturn();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    TimeUnit.MILLISECONDS.sleep(300);
                } catch (InterruptedException e) {
                    //ignore
                }
            }
            addable.print(++count + "");
            System.out.println("结束");
        }).start();
    }
}
