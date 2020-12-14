package org.kin.kinrpc.demo.rpc.reference;

import org.kin.kinrpc.cluster.RpcContext;
import org.kin.kinrpc.demo.rpc.service.Addable;
import org.kin.kinrpc.spring.KinRpcReference;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2020/12/15
 */
@SpringBootApplication
public class AnnotationReference {
    @KinRpcReference()
    private Addable addable;

    @PostConstruct
    public void test() throws InterruptedException {
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

            TimeUnit.MILLISECONDS.sleep(300);
        }
        addable.print(++count + "");
        System.out.println("结束");
    }

    public static void main(String[] args) {
        SpringApplication.run(AnnotationReference.class);
    }
}
