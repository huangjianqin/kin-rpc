package org.kin.kinrpc.demo.message;

import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.config.SerializationType;
import org.kin.kinrpc.message.Actor;
import org.kin.kinrpc.message.ActorEnv;
import org.kin.kinrpc.message.Behaviors;
import org.kin.kinrpc.message.RemotingActorEnv;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author huangjianqin
 * @date 2020-06-13
 */
public class RemotingActorDemo extends Actor {
    public static void main(String[] args) throws IOException {
        ActorEnv actorEnv = RemotingActorEnv.builder2().port(16888).serializationType(SerializationType.KRYO).build();
        String name = "actorDemo";
        RemotingActorDemo actorDemo = new RemotingActorDemo();
        actorEnv.newActor(name, actorDemo);

        //destroy
        System.in.read();
        actorEnv.removeActor(name, actorDemo);
        actorEnv.destroy();
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
                    System.out.println("actorDemo intercept behavior, message=" + message);
                    next.intercept(next, behavior, message);
                })
                .behavior(PrintMessage.class, pm -> {
                    System.out.println(pm.getContent());
                    sender().tell(new ReplyMessage("i have print...." + StringUtils.randomString(ThreadLocalRandom.current().nextInt(20))));
                })
                .behavior(AskMessage.class, am -> {
                    //原路返回
                    sender().answer(new ReplyMessage("hi, " + am.getContent()));
                })
                .build();
    }

    @Override
    public boolean threadSafe() {
        return true;
    }
}
