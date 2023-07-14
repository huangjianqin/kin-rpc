package org.kin.kinrpc.demo.message;

import com.google.common.base.Stopwatch;
import org.kin.framework.JvmCloseCleaner;
import org.kin.kinrpc.config.SerializationType;
import org.kin.kinrpc.message.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2020-06-13
 */
public class RemotingActorRefDemo extends Actor {

    public static void main(String[] args) {
        ActorEnv actorEnv = RemotingActorEnv.builder2().port(16889).serializationType(SerializationType.KRYO).build();
        String name = "actorRefDemo";
        RemotingActorRefDemo actorRefDemo = new RemotingActorRefDemo();
        try {
            actorEnv.newActor(name, actorRefDemo);

            JvmCloseCleaner.instance().add(actorEnv::destroy);

            ActorRef actorDemoRef = actorEnv.actorOf(Address.of(16888), "actorDemo");

            Stopwatch watcher = Stopwatch.createStarted();
            int count = 0;
            while (count < 5) {
                try {
                    actorDemoRef.tell(new PrintMessage(++count + ""));
                    CompletableFuture<ReplyMessage> future = actorDemoRef.ask(new AskMessage(++count + ""));
                    System.out.println("ask with block >>>> " + future.get());

                    actorDemoRef.ask(new AskMessage(++count + ""), new MessageCallback() {
                        @Override
                        public <REQ, RESP> void onSuccess(REQ request, RESP response) {
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
                .interceptors((next, behavior, message) -> {
                    System.out.println("actorRefDemo intercept behavior, message=" + message);
                    next.intercept(next, behavior, message);
                })
                .behavior(ReplyMessage.class, pm -> {
                    System.out.println("remote reply, " + pm.getContent());
                })
                .build();
    }

    @Override
    public boolean threadSafe() {
        return true;
    }
}
