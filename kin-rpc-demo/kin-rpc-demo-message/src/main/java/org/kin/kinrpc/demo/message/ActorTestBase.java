package org.kin.kinrpc.demo.message;

import org.kin.kinrpc.message.ActorEnv;

/**
 * @author huangjianqin
 * @date 2023/7/14
 */
public abstract class ActorTestBase implements Runnable {
    private ActorEnv actorEnv;

    @Override
    public void run() {
        String name = "responder";
        ResponderActor responderActor = new ResponderActor();
        try {
            actorEnv = createActorEnv();
            actorEnv.newActor(name, responderActor);
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //destroy
            actorEnv.removeActor(name, responderActor);
            actorEnv.destroy();
        }
    }

    protected abstract ActorEnv createActorEnv();
}
