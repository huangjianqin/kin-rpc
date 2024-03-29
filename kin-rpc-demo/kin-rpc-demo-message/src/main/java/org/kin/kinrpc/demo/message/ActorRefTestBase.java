package org.kin.kinrpc.demo.message;

import com.google.common.base.Stopwatch;
import org.kin.framework.JvmCloseCleaner;
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
    /** 阻塞等待user shutdown */
    private final boolean block;

    protected ActorRefTestBase(boolean block) {
        this.block = block;
    }

    @Override
    public void run() {
        actorEnv = createActorEnv();
        JvmCloseCleaner.instance().add(() -> actorEnv.destroy());
        String name = "requester";
        RequestActor requestActor = new RequestActor();
        try {
            actorEnv.newActor(name, requestActor);

            ActorRef responderActor = createResponderActor(actorEnv);

            Stopwatch watcher = Stopwatch.createStarted();
            int count = 0;
            while (count < 5) {
                try {
                    responderActor.tell(new PrintMessage(++count + ""), requestActor.self());
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
            if (block) {
                System.in.read();
            } else {
                Thread.sleep(3_000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (block) {
                //destroy
                actorEnv.removeActor(name, requestActor);
                actorEnv.destroy();
                try {
                    Thread.sleep(2_000);
                } catch (InterruptedException e) {
                    //ignore
                }
                System.out.println("force exit");
                System.exit(0);
            }
        }
    }

    protected abstract ActorEnv createActorEnv();

    protected abstract ActorRef createResponderActor(ActorEnv actorEnv);
}
