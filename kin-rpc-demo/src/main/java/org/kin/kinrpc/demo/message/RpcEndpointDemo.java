package org.kin.kinrpc.demo.message;

import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.utils.SysUtils;
import org.kin.kinrpc.message.core.RpcEndpoint;
import org.kin.kinrpc.message.core.RpcEnv;
import org.kin.kinrpc.message.core.RpcMessageCallContext;
import org.kin.kinrpc.transport.serializer.SerializerType;
import org.kin.kinrpc.transport.serializer.Serializers;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-06-13
 */
public class RpcEndpointDemo extends RpcEndpoint {
    public RpcEndpointDemo(RpcEnv rpcEnv) {
        super(rpcEnv);
    }

    public static void main(String[] args) {
        RpcEnv rpcEnv = new RpcEnv("0.0.0.0", 16888, SysUtils.CPU_NUM,
                Serializers.getSerializer(SerializerType.KRYO.getCode()), false);
        rpcEnv.startServer();
        String name = "rpcEndpointDemo";
        RpcEndpointDemo rpcEndpointDemo = new RpcEndpointDemo(rpcEnv);
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
        Serializable message = context.getMessage();
        System.out.println(message);
        context.reply(new ReplyMessage(context.getRequestId()));
        if (message instanceof RpcEndpointRefDemo.PrintMessage) {
            RpcEndpointRefDemo.PrintMessage printMessage = (RpcEndpointRefDemo.PrintMessage) message;
            printMessage.getFrom().send(new ReplyMessage(context.getRequestId()));
        }
    }

    @Override
    public boolean threadSafe() {
        return true;
    }

    public static class ReplyMessage implements Serializable {
        private static final long serialVersionUID = -1586292592951384110L;
        private long requestId;

        public ReplyMessage(long requestId) {
            this.requestId = requestId;
        }

        public long getRequestId() {
            return requestId;
        }

        public void setRequestId(long requestId) {
            this.requestId = requestId;
        }

        @Override
        public String toString() {
            return "ReplyMessage{" +
                    "requestId='" + requestId + '\'' +
                    '}';
        }
    }
}
