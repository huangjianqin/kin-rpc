package org.kin.kinrpc.demo.message;

import com.google.common.base.Stopwatch;
import org.kin.framework.JvmCloseCleaner;
import org.kin.kinrpc.message.core.*;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;
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

        JvmCloseCleaner.instance().add(() -> {
            rpcEnv.unregister(name, rpcEndpointRefDemo);
            rpcEnv.stop();
        });

        RpcEndpointRef endpointRef = rpcEnv.createEndpointRef("0.0.0.0", 16888, "rpcEndpointDemo");

        Stopwatch watcher = Stopwatch.createStarted();
        int count = 0;
        while (count < 100000) {
            try {
                RpcEndpointRef self = rpcEndpointRefDemo.ref();
                endpointRef.fireAndForget(new PrintMessage(++count + "", self));
                CompletableFuture<RpcEndpointDemo.ReplyMessage> future = endpointRef.requestResponse(new AskMessage(++count + ""));
                System.out.println("ask with block >>>> " + future.get());

                endpointRef.requestResponse(new AskMessage(++count + ""), new RpcCallback() {

                    @Override
                    public <REQ extends Serializable, RESP extends Serializable> void onResponse(long requestId, REQ request, RESP response) {
                        System.out.println("ask with timeout >>>> " + requestId + "~~~~~" + request + "~~~~~" + response);
                    }

                    @Override
                    public void onException(Throwable e) {
                        System.err.println("ask with timeout >>>> " + e);
                    }
                }, 3_000);
            } catch (Exception e) {
                System.err.println(e);
            }
        }
        watcher.stop();
        System.out.printf("结束, 耗时%d ms%n", watcher.elapsed(TimeUnit.MILLISECONDS));
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
        System.out.println("receive >>>> " + context.getMessage());
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


    public static class AskMessage implements Serializable {
        private static final long serialVersionUID = -6807586672826372145L;

        private String content;

        public AskMessage(String content) {
            this.content = content;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        @Override
        public String toString() {
            return "AskMessage{" +
                    "content='" + content + '\'' +
                    '}';
        }
    }
}
