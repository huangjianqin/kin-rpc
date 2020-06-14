package org.kin.kinrpc.rpc.transport;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.kin.kinrpc.transport.domain.RpcRequestIdGenerator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Created by 健勤 on 2017/2/25.
 */
public class RpcRequestKryoSerializeTest {
    public static void main(String[] args) {
        RpcRequest request = new RpcRequest(RpcRequestIdGenerator.next(), "system.service.Addable", "add", new Object[]{1, 1});

        Kryo kryo = new Kryo();
        Output output = new Output(new ByteArrayOutputStream());
        kryo.writeObject(output, request);
        byte[] bytes = output.toBytes();
        output.close();

        Input input = new Input(new ByteArrayInputStream(bytes));
        RpcRequest request1 = kryo.readObject(input, RpcRequest.class);
        input.close();

        System.out.println(request == request1);
        System.out.println(request.equals(request1));
    }
}