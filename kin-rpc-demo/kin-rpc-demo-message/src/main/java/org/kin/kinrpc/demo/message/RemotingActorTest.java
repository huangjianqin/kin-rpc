package org.kin.kinrpc.demo.message;

import org.kin.kinrpc.message.ActorEnv;
import org.kin.kinrpc.message.RemotingActorEnv;

/**
 * @author huangjianqin
 * @date 2020-06-13
 */
public class RemotingActorTest extends ActorTestBase {
    public static void main(String[] args) {
        new RemotingActorTest().run();
    }

    @Override
    protected ActorEnv createActorEnv() {
        return RemotingActorEnv.builder2().port(16888).build();
    }
}
