package org.kin.kinrpc.demo.message;

import org.kin.framework.JvmCloseCleaner;
import org.kin.kinrpc.message.core.*;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2020-06-13
 */
public class RpcEndpointRefDemo extends RpcEndpoint {
    public RpcEndpointRefDemo(RpcEnv rpcEnv) {
        super(rpcEnv);
    }

    public static void main(String[] args) throws InterruptedException {
        RpcEnv rpcEnv = new RpcEnv("0.0.0.0", 16889);
        rpcEnv.startServer();
        String name = "rpcEndpointRefDemo";
        RpcEndpointRefDemo rpcEndpointRefDemo = new RpcEndpointRefDemo(rpcEnv);
        rpcEnv.register(name, rpcEndpointRefDemo);

        JvmCloseCleaner.DEFAULT().add(() -> {
            rpcEnv.unregister(name, rpcEndpointRefDemo);
            rpcEnv.stop();
        });

        RpcEndpointRef endpointRef = rpcEnv.createEndpointRef("0.0.0.0", 16888, "rpcEndpointDemo");

        int count = 0;
        while (count < 10000) {
            try {
                RpcEndpointRef self = rpcEnv.rpcEndpointRef(rpcEndpointRefDemo);
                endpointRef.send(new PrintMessage(++count + "", self));
                RpcFuture<RpcEndpointDemo.ReplyMessage> future = endpointRef.ask(new PrintMessage(++count + "", self));
                System.out.println(future.get());
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
    protected void onReceiveMessage(MessagePostContext context) {
        System.err.println(context.getMessage());
    }

    @Override
    public boolean threadSafe() {
        return true;
    }

    public static class PrintMessage implements Serializable {
        private static final long serialVersionUID = -1632194863001778858L;

        private String content;
        private RpcEndpointRef from;

        public PrintMessage() {
        }

        public PrintMessage(String content, RpcEndpointRef from) {
            this.content = content;
            this.from = from;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public RpcEndpointRef getFrom() {
            return from;
        }

        public void setFrom(RpcEndpointRef from) {
            this.from = from;
        }

        @Override
        public String toString() {
            return "PrintMessage{" +
                    "content='" + content + '\'' +
                    ", rpcEndpointRef=" + from +
                    '}';
        }
    }
}
