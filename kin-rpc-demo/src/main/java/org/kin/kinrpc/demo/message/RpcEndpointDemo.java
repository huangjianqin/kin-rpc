package org.kin.kinrpc.demo.message;

import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.utils.SysUtils;
import org.kin.kinrpc.config.SerializerType;
import org.kin.kinrpc.message.core.RpcEndpoint;
import org.kin.kinrpc.message.core.RpcEnv;
import org.kin.kinrpc.message.core.RpcMessageCallContext;
import org.kin.kinrpc.transport.serializer.Serializers;

import java.io.Serializable;

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
        System.out.println(context.getMessage());
        context.reply(new ReplyMessage(context.getRequestId()));
    }

    @Override
    public boolean threadSafe() {
        return true;
    }

    private class ReplyMessage implements Serializable {
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
