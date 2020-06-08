package org.kin.kinrpc.rpc.transport;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import org.kin.kinrpc.domain.RpcRequestIdGenerator;
import org.kin.kinrpc.rpc.transport.domain.RpcRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by 健勤 on 2017/2/9.
 */
public class RpcRequestHessianSerializeTest {
    public static void main(String[] args) throws IOException {
        RpcRequest request = new RpcRequest(RpcRequestIdGenerator.next(), "system.service.Addable", "add", new Object[]{1, 1});

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Hessian2Output hessian2Output = new Hessian2Output(baos);
        hessian2Output.writeObject(request);

        hessian2Output.close();
        baos.close();

        System.out.println(baos.toByteArray().length);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        Hessian2Input hessian2Input = new Hessian2Input(bais);
        RpcRequest cloneRequest = (RpcRequest) hessian2Input.readObject();

        System.out.println(cloneRequest.toString());

        hessian2Input.close();
        bais.close();

    }
}


