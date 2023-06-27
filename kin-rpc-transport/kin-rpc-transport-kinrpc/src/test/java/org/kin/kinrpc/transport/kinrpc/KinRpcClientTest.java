package org.kin.kinrpc.transport.kinrpc;

import org.kin.kinrpc.transport.cmd.MessageCommand;
import org.kin.kinrpc.transport.cmd.RpcRequestCommand;
import org.kin.kinrpc.transport.cmd.RpcResponseCommand;

import java.io.IOException;

/**
 * @author huangjianqin
 * @date 2023/6/7
 */
public class KinRpcClientTest {
    public static void main(String[] args) throws IOException, InterruptedException {
        KinRpcClient client = new KinRpcClient(19999);
        client.connect();

        try {
            while (!client.isAvailable()) {
                Thread.sleep(1_000);
            }

            RpcRequestCommand rpcRequestCommand = new RpcRequestCommand((byte) 4, 0, 0, new Object[]{new String("Hello rpc")});
            RpcResponseCommand rpcResponseCommand = client.bRequestResponse(rpcRequestCommand);
            rpcResponseCommand.deserializeResult(String.class);
            System.out.println(rpcResponseCommand.getResult());

            MessageCommand messageCommand = new MessageCommand((byte) 4, "test", new String("Hello message"));
            String messageResponse = client.bRequestResponse(messageCommand);
            System.out.println(messageResponse);

            System.in.read();
        } finally {
            client.shutdown();
            System.exit(0);
        }
    }
}