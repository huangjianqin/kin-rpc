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

    public static void main(String[] args) {
        ActorEnv actorEnv = ActorEnv.builder().port(16889).build();
        String name = "actorRefDemo";
        ActorRefDemo actorRefDemo = new ActorRefDemo();
        try {
            actorEnv.newActor(name, actorRefDemo);

            JvmCloseCleaner.instance().add(actorEnv::destroy);

            ActorRef actorDemoRef = actorEnv.actorOf(Address.of(16888), "actorDemo");

            Stopwatch watcher = Stopwatch.createStarted();
            int count = 0;
            while (count < 5) {
                try {
                    // TODO: 2023/7/13
//                    actorDemoRef.fireAndForget(new PrintMessage(++count + ""));
                    CompletableFuture<ActorDemo.ReplyMessage> future = actorDemoRef.requestResponse(new AskMessage(++count + ""));
                    System.out.println("ask with block >>>> " + future.get());

                    actorDemoRef.requestResponse(new AskMessage(++count + ""), new MessageCallback() {
                        @Override
                        public <REQ extends Serializable, RESP extends Serializable> void onSuccess(REQ request, RESP response) {
                            System.out.println("ask with timeout >>>> " + "~~~~~" + request + "~~~~~" + response);
                        }

                        @Override
                        public void onFailure(Throwable e) {
                            System.err.println("ask with timeout >>>> " + e);
                        }
                    }, 2_000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            watcher.stop();
            System.out.printf("结束, 耗时%d ms%n", watcher.elapsed(TimeUnit.MILLISECONDS));
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            actorEnv.removeActor(name, actorRefDemo);
            actorEnv.destroy();
        }
        System.exit(0);
    }

    @Override
    protected void onStart() {
        System.out.println("actor start");
    }

    @Override
    protected void onStop() {
        System.out.println("actor stop");
    }

    @Override
    protected Behaviors createBehaviors() {
        return Behaviors.builder()
                .interceptors((next, behavior, actorContext, message) -> {
                    System.out.println("actorRefDemo intercept behavior, message=" + message);
                    next.intercept(next, behavior, actorContext, message);
                })
                .behavior(ActorDemo.ReplyMessage.class, (ac, pm) -> {
                    System.out.println("remote reply, " + pm.getContent());
                })
                .build();
    }

    @Override
    public boolean threadSafe() {
        return true;
    }

    public static class PrintMessage implements Serializable {
        private static final long serialVersionUID = -1632194863001778858L;

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

        @Override
        public String toString() {
            return "PrintMessage{" +
                    "content='" + content + '\'' +
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
