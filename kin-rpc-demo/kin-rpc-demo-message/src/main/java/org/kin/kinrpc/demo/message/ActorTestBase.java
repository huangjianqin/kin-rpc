package org.kin.kinrpc.demo.message;

import org.kin.kinrpc.message.ActorEnv;

/**
 * @author huangjianqin
 * @date 2023/7/14
 */
public abstract class ActorTestBase implements Runnable {
    private ActorEnv actorEnv;
    private final boolean block;

    protected ActorTestBase(boolean block) {
        this.block = block;
    }

    @Override
    public void run() {
        String name = "responder";
        ResponderActor responderActor = new ResponderActor();
        try {
            actorEnv = createActorEnv();
            actorEnv.newActor(name, responderActor);
            if (block) {
                System.in.read();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (block) {
                //destroy
                actorEnv.removeActor(name, responderActor);
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
}
