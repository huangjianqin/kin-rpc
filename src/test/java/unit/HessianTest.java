package unit;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import org.kinrpc.rpc.protol.RPCRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;

/**
 * Created by 健勤 on 2017/2/9.
 */
public class HessianTest {
    public static void main(String[] args) throws IOException {
        RPCRequest request = new RPCRequest(0, "system.service.Addable", "add", new Object[]{1,1});

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Hessian2Output hessian2Output = new Hessian2Output(baos);
        hessian2Output.writeObject(request);

        hessian2Output.close();
        baos.close();

        System.out.println(baos.toByteArray().length);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        Hessian2Input hessian2Input = new Hessian2Input(bais);
        RPCRequest cloneRequest = (RPCRequest)hessian2Input.readObject();

        System.out.println(cloneRequest.toString());

        hessian2Input.close();
        bais.close();

    }
}


