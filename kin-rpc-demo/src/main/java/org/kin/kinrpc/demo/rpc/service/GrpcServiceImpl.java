package org.kin.kinrpc.demo.rpc.service;

import io.grpc.stub.StreamObserver;
import org.kin.kinrpc.demo.rpc.KinRpcGrpcServiceGrpc;
import org.kin.kinrpc.demo.rpc.Notify;
import org.kin.kinrpc.demo.rpc.Num;

/**
 * @author huangjianqin
 * @date 2020/12/2
 */
public class GrpcServiceImpl extends KinRpcGrpcServiceGrpc.GrpcServiceImplBase {
    @Override
    public void add(Num request, StreamObserver<Num> responseObserver) {
        int sum = request.getNum1() + request.getNum2();
        Num result = Num.newBuilder().setNum1(sum).setNum2(sum).build();

        responseObserver.onNext(result);
        responseObserver.onCompleted();
    }

    @Override
    public void notify(Num request, StreamObserver<Notify> responseObserver) {
        Notify notify = Notify.newBuilder()
                .setContent(request.getNum1() + "" + request.getNum2()).build();
        responseObserver.onNext(notify);
        responseObserver.onCompleted();
    }
}