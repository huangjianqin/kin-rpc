package org.kin.kinrpc.demo.message;

import com.google.common.base.Stopwatch;
import org.kin.kinrpc.message.ActorEnv;
import org.kin.kinrpc.message.ActorRef;
import org.kin.kinrpc.message.MessageCallback;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2023/7/14
 */
public abstract class ActorRefTestBase implements Runnable {
    private ActorEnv actorEnv;

    @Override
    public void run() {
        actorEnv = createActorEnv();
        String name = "requester";
        RequestActor requestActor = new RequestActor();
        try {
            actorEnv.newActor(name, requestActor);

            ActorRef responderActor = createResponderActor(actorEnv);

            Stopwatch watcher = Stopwatch.createStarted();
            int count = 0;
            while (count < 5) {
                try {
                    responderActor.tell(new PrintMessage(++count + ""));
                    CompletableFuture<ReplyMessage> future = responderActor.ask(new AskMessage(++count + ""));
                    System.out.println("ask with block >>>> " + future.get());

                    responderActor.ask(new AskMessage(++count + ""), new MessageCallback() {
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
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            actorEnv.removeActor(name, requestActor);
            actorEnv.destroy();
        }
        System.exit(0);
    }

    protected abstract ActorEnv createActorEnv();

    protected abstract ActorRef createResponderActor(ActorEnv actorEnv);
}
