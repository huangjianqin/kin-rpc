package org.kin.kinrpc.demo.message;

import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.utils.SysUtils;
import org.kin.kinrpc.config.SerializerType;
import org.kin.kinrpc.message.core.RpcEndpoint;
import org.kin.kinrpc.message.core.RpcEnv;
import org.kin.kinrpc.message.core.RpcMessageCallContext;
import org.kin.kinrpc.transport.serializer.Serializers;

/**
 * @author huangjianqin
 * @date 2020-06-13
 */
public class RpcEndpointDemo extends RpcEndpoint {
    public static void main(String[] args) {
        RpcEnv rpcEnv = new RpcEnv("0.0.0.0", 16888, SysUtils.CPU_NUM,
                Serializers.getSerializer(SerializerType.KRYO.name()), false);
        rpcEnv.startServer();
        String name = "rpcEndpointDemo";
        RpcEndpointDemo rpcEndpointDemo = new RpcEndpointDemo();
        rpcEnv.register(name, rpcEndpointDemo);

        JvmCloseCleaner.DEFAULT().add(() -> {
            rpcEnv.unregister(name, rpcEndpointDemo);
            rpcEnv.stop();
        });
    }

    @Override
    protected void onStart() {
        System.out.println("endpoint start");
    }

    @Override
    protected void onStop() {
        System.out.println("endpoint stop");
    }

    @Override
    public void receive(RpcMessageCallContext context) {
        super.receive(context);
        System.out.println(context);
    }

    @Override
    public boolean threadSafe() {
        return true;
    }
}
