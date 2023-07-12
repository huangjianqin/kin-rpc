package org.kin.kinrpc.demo.message;

import com.google.common.base.Stopwatch;
import org.kin.framework.JvmCloseCleaner;
import org.kin.kinrpc.message.*;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2020-06-13
 */
public class ActorRefDemo extends Actor {
    public ActorRefDemo(ActorEnv actorEnv) {
        super(actorEnv);
    }

    public static void main(String[] args) throws InterruptedException {
        ActorEnv actorEnv = new ActorEnv("0.0.0.0", 16889);
        actorEnv.startServer();
        String name = "rpcEndpointRefDemo";
        ActorRefDemo rpcEndpointRefDemo = new ActorRefDemo(actorEnv);
        actorEnv.newActor(name, rpcEndpointRefDemo);

        JvmCloseCleaner.instance().add(() -> {
            actorEnv.removeActor(name, rpcEndpointRefDemo);
            actorEnv.destroy();
        });

        ActorRef endpointRef = actorEnv.actorOf("0.0.0.0", 16888, "rpcEndpointDemo");

        Stopwatch watcher = Stopwatch.createStarted();
        int count = 0;
        while (count < 100000) {
            try {
                ActorRef self = rpcEndpointRefDemo.ref();
                endpointRef.fireAndForget(new PrintMessage(++count + "", self));
                CompletableFuture<ActorDemo.ReplyMessage> future = endpointRef.requestResponse(new AskMessage(++count + ""));
                System.out.println("ask with block >>>> " + future.get());

                endpointRef.requestResponse(new AskMessage(++count + ""), new MessageCallback() {

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
        private ActorRef from;

        public PrintMessage() {
        }

        public PrintMessage(String content, ActorRef from) {
            this.content = content;
            this.from = from;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public ActorRef getFrom() {
            return from;
        }

        public void setFrom(ActorRef from) {
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
