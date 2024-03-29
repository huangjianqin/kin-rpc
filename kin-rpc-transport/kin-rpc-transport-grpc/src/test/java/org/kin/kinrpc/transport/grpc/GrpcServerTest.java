package org.kin.kinrpc.transport.grpc;

import org.kin.kinrpc.transport.AbstractRequestProcessor;
import org.kin.kinrpc.transport.RequestContext;
import org.kin.kinrpc.transport.RpcRequestProcessor;
import org.kin.kinrpc.transport.cmd.RpcRequestCommand;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author huangjianqin
 * @date 2023/6/7
 */
public class GrpcServerTest {
    public static void main(String[] args) throws IOException {
        GrpcServer server = new GrpcServer(19999);
        server.registerService(0, 0);
        server.registerRequestProcessor(new RpcRequestProcessor() {
                    @Override
                    public void process(RequestContext requestContext, RpcRequestCommand request) {
                        request.deserializeParams(String.class);
                        System.out.println(Arrays.toString(request.getParams()));
                        requestContext.writeResponse("Welcome to rpc");
                    }
                })
                .registerRequestProcessor(new AbstractRequestProcessor<String>() {
                    @Override
                    public void process(RequestContext requestContext, String request) {
                        System.out.println(request);
                        requestContext.writeMessageResponse("Welcome to message");
                    }
                });
        server.start();
        System.in.read();
        server.shutdown();
    }
}
