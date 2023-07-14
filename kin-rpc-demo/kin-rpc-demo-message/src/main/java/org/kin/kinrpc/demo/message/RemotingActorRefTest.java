package org.kin.kinrpc.demo.message;

import org.kin.kinrpc.message.ActorEnv;
import org.kin.kinrpc.message.ActorRef;
import org.kin.kinrpc.message.RemotingActorEnv;

/**
 * @author huangjianqin
 * @date 2020-06-13
 */
public class RemotingActorRefTest extends ActorRefTestBase {

    public static void main(String[] args) {
        new RemotingActorRefTest().run();
    }

    @Override
    protected ActorEnv createActorEnv() {
        return RemotingActorEnv.builder2().port(16889).build();
    }

    @Override
    protected ActorRef createResponderActor(ActorEnv actorEnv) {
        return actorEnv.actorOf("responder");
    }
}
