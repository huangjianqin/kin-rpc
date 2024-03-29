package org.kin.kinrpc.transport.grpc;

import org.kin.kinrpc.transport.cmd.MessageCommand;
import org.kin.kinrpc.transport.cmd.RpcRequestCommand;
import org.kin.kinrpc.transport.cmd.RpcResponseCommand;

import java.io.IOException;

/**
 * @author huangjianqin
 * @date 2023/6/7
 */
public class GrpcClientTest {
    public static void main(String[] args) throws IOException {
        GrpcClient client = new GrpcClient(19999);
        client.connect();

        try {
            RpcRequestCommand rpcRequestCommand = new RpcRequestCommand((byte) 9, 0, 0, new Object[]{new String("Hello rpc")});
            RpcResponseCommand rpcResponseCommand = client.bRequestResponse(rpcRequestCommand);
            rpcResponseCommand.deserializeResult(String.class);
            System.out.println(rpcResponseCommand.getResult());

            MessageCommand messageCommand = new MessageCommand((byte) 9, new String("Hello message"));
            String messageResponse = client.bRequestResponse(messageCommand);
            System.out.println(messageResponse);

            System.in.read();
        } finally {
            client.shutdown();
        }
    }
}
