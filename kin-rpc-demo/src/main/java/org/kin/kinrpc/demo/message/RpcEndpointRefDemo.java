package org.kin.kinrpc.demo.message;

import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.utils.SysUtils;
import org.kin.kinrpc.config.SerializerType;
import org.kin.kinrpc.message.core.RpcEndpoint;
import org.kin.kinrpc.message.core.RpcEndpointRef;
import org.kin.kinrpc.message.core.RpcEnv;
import org.kin.kinrpc.message.core.RpcMessageCallContext;
import org.kin.kinrpc.message.transport.domain.RpcEndpointAddress;
import org.kin.kinrpc.transport.domain.RpcAddress;
import org.kin.kinrpc.transport.serializer.Serializers;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2020-06-13
 */
public class RpcEndpointRefDemo extends RpcEndpoint {
    public static void main(String[] args) throws InterruptedException {
        RpcEnv rpcEnv = new RpcEnv("0.0.0.0", 16889, SysUtils.CPU_NUM,
                Serializers.getSerializer(SerializerType.KRYO.name()), false);
        rpcEnv.startServer();
        String name = "rpcEndpointRefDemo";
        RpcEndpointRefDemo rpcEndpointRefDemo = new RpcEndpointRefDemo();
        rpcEnv.register(name, rpcEndpointRefDemo);

        JvmCloseCleaner.DEFAULT().add(() -> {
            rpcEnv.unregister(name, rpcEndpointRefDemo);
            rpcEnv.stop();
        });

        RpcEndpointRef endpointRef = new RpcEndpointRef(
                RpcEndpointAddress.of(
                        RpcAddress.of("0.0.0.0", 16888), "Master"));
        endpointRef.updateRpcEnv(rpcEnv);

        int count = 0;
        while (count < 10000) {
            try {
                endpointRef.send(endpointRef, new PrintMessage(++count + ""));
            } catch (Exception e) {
                System.err.println(e);
            }

            TimeUnit.MILLISECONDS.sleep(300);
        }
        System.out.println("结束");
        System.exit(0);
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

    private static class PrintMessage implements Serializable {
        private String content;

        public PrintMessage() {
        }

        public PrintMessage(String content) {
            this.content = content;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
