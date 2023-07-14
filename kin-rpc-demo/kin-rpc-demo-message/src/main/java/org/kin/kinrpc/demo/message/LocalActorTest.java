package org.kin.kinrpc.demo.message;

import org.kin.kinrpc.message.ActorEnv;
import org.kin.kinrpc.message.ActorRef;
import org.kin.kinrpc.message.RemotingActorEnv;

import java.util.concurrent.ForkJoinPool;

/**
 * @author huangjianqin
 * @date 2023/7/14
 */
public class LocalActorTest {

    public static void main(String[] args) throws InterruptedException {
        ActorEnv actorEnv = RemotingActorEnv.builder().build();
        ActorTestBase actorTestBase = new ActorTestBase() {
            @Override
            protected ActorEnv createActorEnv() {
                return actorEnv;
            }
        };
        ActorRefTestBase actorRefTestBase = new ActorRefTestBase() {
            @Override
            protected ActorEnv createActorEnv() {
                return actorEnv;
            }

            @Override
            protected ActorRef createResponderActor(ActorEnv actorEnv) {
                return actorEnv.actorOf("responder");

            }
        };

        ForkJoinPool.commonPool()
                .execute(actorTestBase);
        Thread.sleep(1_000);
        ForkJoinPool.commonPool()
                .execute(actorRefTestBase);
        Thread.sleep(5_000);
        System.exit(0);
    }
}
