package org.kin.kinrpc.demo.rpc.grpc;

import javassist.NotFoundException;
import org.kin.framework.proxy.ProxyEnhanceUtils;
import org.kin.kinrpc.cluster.RpcContext;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.References;
import org.kin.kinrpc.rpc.Notifier;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2020/12/2
 */
public class GrpcAddableReference {
    public static void main(String[] args) throws InterruptedException {
        try {
            ProxyEnhanceUtils.getPool().get("org.kin.kinrpc.demo.rpc.grpc.KinRpcGrpcServiceGrpc$KinRpcGrpcServiceStub");
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
        ReferenceConfig<KinRpcGrpcServiceGrpc.GrpcService> config = References.reference(KinRpcGrpcServiceGrpc.GrpcService.class)
                .version("001")
                .async()
                .notify(GrpcNotifier.N)
                .callTimeout(2000)
                .rate(10000);
        config.urls("grpc://0.0.0.0:16888");
        KinRpcGrpcServiceGrpc.GrpcService service = config.get();
        int count = 0;
        while (count < 10000) {
            try {
                service.add(Num.newBuilder().setNum1(1).setNum2(1).build());
                CompletableFuture<Object> future = RpcContext.future();
                System.out.println("结果" + future.get());

                service.notify(Num.newBuilder().setNum1(2).setNum2(2).build());
            } catch (Exception e) {
                e.printStackTrace();
            }

            TimeUnit.MILLISECONDS.sleep(300);
        }
        System.out.println("结束");
        config.disable();
        System.exit(0);
    }

    public static class GrpcNotifier implements Notifier<Notify> {
        public static final GrpcNotifier N = new GrpcNotifier();

        @Override
        public void onRpcCallSuc(Notify obj) {
            System.out.println(obj.toString());
        }

        @Override
        public void handlerException(Throwable throwable) {
            throwable.printStackTrace();
        }
    }
}
