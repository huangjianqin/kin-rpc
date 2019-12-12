package org.kin.kinrpc.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.kin.kinrpc.rpc.transport.domain.RPCRequest;
import org.kin.kinrpc.rpc.transport.domain.RPCRequestIdGenerator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Created by 健勤 on 2017/2/25.
 */
public class KryoTest {
    public static void main(String[] args) {
        RPCRequest request = new RPCRequest(RPCRequestIdGenerator.next(), "system.service.Addable", "add", new Object[]{1, 1});

        Kryo kryo = new Kryo();
        Output output = new Output(new ByteArrayOutputStream());
        kryo.writeObject(output, request);
        byte[] bytes = output.toBytes();
        output.close();

        Input input = new Input(new ByteArrayInputStream(bytes));
        RPCRequest request1 = kryo.readObject(input, RPCRequest.class);
        input.close();

        System.out.println(request == request1);
        System.out.println(request.equals(request1));
    }
}
